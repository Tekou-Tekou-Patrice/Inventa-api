package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.PayrollDueResponse;
import static com.onescompany.Inventa.api.ApiDtos.PaymentResponse;
import static com.onescompany.Inventa.api.ApiDtos.PayrollPaymentDecisionRequest;
import static com.onescompany.Inventa.api.ApiDtos.PayrollPaymentMarkPaidRequest;
import static com.onescompany.Inventa.api.ApiDtos.PayrollPaymentRequestCreate;
import static com.onescompany.Inventa.api.ApiDtos.PayrollPaymentRequestResponse;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.domain.AttendanceRecord;
import com.onescompany.Inventa.domain.AttendanceRepository;
import com.onescompany.Inventa.domain.DailyReportValidationRepository;
import com.onescompany.Inventa.domain.PayrollSettings;
import com.onescompany.Inventa.domain.PayrollSettingsRepository;
import com.onescompany.Inventa.domain.Payment;
import com.onescompany.Inventa.domain.PaymentRepository;
import com.onescompany.Inventa.domain.PayrollPaymentRequest;
import com.onescompany.Inventa.domain.PayrollPaymentRequestRepository;
import com.onescompany.Inventa.domain.Worker;
import com.onescompany.Inventa.domain.WorkerRepository;
import com.onescompany.Inventa.security.PushNotificationService;
import com.onescompany.Inventa.security.UserRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payroll")
@Transactional
public class PayrollSettlementController {
    private final WorkerRepository workers;
    private final AttendanceRepository attendance;
    private final PaymentRepository payments;
    private final PayrollSettingsRepository settings;
    private final DailyReportValidationRepository reportValidations;
    private final PayrollPaymentRequestRepository payrollRequests;
    private final PushNotificationService pushNotifications;

    public PayrollSettlementController(
            WorkerRepository workers,
            AttendanceRepository attendance,
            PaymentRepository payments,
            PayrollSettingsRepository settings,
            DailyReportValidationRepository reportValidations,
            PayrollPaymentRequestRepository payrollRequests,
            PushNotificationService pushNotifications) {
        this.workers = workers;
        this.attendance = attendance;
        this.payments = payments;
        this.settings = settings;
        this.reportValidations = reportValidations;
        this.payrollRequests = payrollRequests;
        this.pushNotifications = pushNotifications;
    }

    @GetMapping("/due")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public List<PayrollDueResponse> due(@RequestParam YearMonth through) {
        return workers.findAll().stream()
                .map(worker -> dueFor(worker, through))
                .toList();
    }

    @PostMapping("/workers/{workerId}/payments")
    @PreAuthorize("hasRole('ASSISTANT')")
    public PaymentResponse recordPayment(
            @PathVariable Long workerId,
            @RequestParam YearMonth through,
            Principal principal) {
        Worker worker = workers.findByIdForUpdate(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));
        PayrollDueResponse due = dueFor(worker, through);
        if (due.amount() <= 0 || due.fromDate() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No accrued payroll is due for this worker.");
        }

        LocalDate paidDate = LocalDate.now(ZoneId.systemDefault());
        Payment payment = payments.save(new Payment(
                worker, due.amount(), principal.getName(), due.throughDate(), paidDate));
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Salaire payé",
                worker.getName() + " · " + due.amount() + " F CFA",
                "payroll_paid");
        reportValidations.deleteById(paidDate);
        return new PaymentResponse(
                payment.getId(),
                worker.getId(),
                worker.getName(),
                payment.getAmount(),
                payment.getRecordedAt(),
                payment.getRecordedBy(),
                worker.getField() == null ? null : worker.getField().getId(),
                worker.getField() == null ? null : worker.getField().getName(),
                payment.getPaidThroughEnd());
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public List<PayrollPaymentRequestResponse> requests() {
        return payrollRequests.findTop100ByOrderByRequestedAtDesc().stream()
                .map(this::payrollRequestResponse)
                .toList();
    }

    @PostMapping("/requests")
    @PreAuthorize("hasRole('ASSISTANT')")
    public PayrollPaymentRequestResponse requestPayment(
            @Valid @RequestBody PayrollPaymentRequestCreate request,
            Principal principal) {
        Worker worker = workers.findByIdForUpdate(request.workerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (request.scheduledPaymentDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The planned payment date cannot be in the past.");
        }
        if (payrollRequests.existsByWorker_IdAndStatusIn(
                worker.getId(),
                List.of(PayrollPaymentRequest.Status.PENDING, PayrollPaymentRequest.Status.APPROVED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A payment request is already awaiting completion.");
        }
        PayrollDueResponse due = dueFor(worker, request.through());
        if (due.amount() <= 0 || due.fromDate() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No accrued payroll is due for this worker.");
        }
        PayrollPaymentRequest saved = payrollRequests.save(new PayrollPaymentRequest(
                worker,
                new PayrollPaymentRequest.PayrollDueData(
                        due.amount(), due.workedMinutes(), due.fromDate(), due.throughDate()),
                request.scheduledPaymentDate(),
                principal.getName()));
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Demande de paiement à valider",
                worker.getName() + " · " + due.amount() + " F CFA",
                "payroll_request");
        return payrollRequestResponse(saved);
    }

    @PutMapping("/requests/{id}/decision")
    @PreAuthorize("hasRole('DIRECTOR')")
    public PayrollPaymentRequestResponse decidePayment(
            @PathVariable Long id,
            @Valid @RequestBody PayrollPaymentDecisionRequest request,
            Principal principal) {
        PayrollPaymentRequest paymentRequest = payrollRequests.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment request not found."));
        if (paymentRequest.getStatus() != PayrollPaymentRequest.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment request has already been decided.");
        }
        paymentRequest.decide(request.approved(), principal.getName());
        pushNotifications.notifyUsername(
                paymentRequest.getRequestedBy(),
                request.approved() ? "Paiement approuvé" : "Paiement refusé",
                paymentRequest.getWorker().getName(),
                request.approved() ? "payroll_approved" : "payroll_rejected");
        return payrollRequestResponse(paymentRequest);
    }

    @PutMapping("/requests/{id}/paid")
    @PreAuthorize("hasRole('ASSISTANT')")
    public PayrollPaymentRequestResponse markPaymentPaid(
            @PathVariable Long id,
            @Valid @RequestBody PayrollPaymentMarkPaidRequest request,
            Principal principal) {
        PayrollPaymentRequest paymentRequest = payrollRequests.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment request not found."));
        if (paymentRequest.getStatus() != PayrollPaymentRequest.Status.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The director must approve this payment first.");
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (request.paidDate().isBefore(paymentRequest.getScheduledPaymentDate()) ||
                request.paidDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The actual payment date is invalid.");
        }
        PayrollDueResponse currentDue = dueFor(
                paymentRequest.getWorker(), YearMonth.from(paymentRequest.getThroughDate()));
        if (currentDue.amount() != paymentRequest.getAmount()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Payroll data changed after the request; create a new request.");
        }
        paymentRequest.markPaid(request.paidDate(), principal.getName());
        payments.save(new Payment(
                paymentRequest.getWorker(),
                paymentRequest.getAmount(),
                principal.getName(),
                paymentRequest.getThroughDate(),
                request.paidDate()));
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Salaire payé",
                paymentRequest.getWorker().getName() + " · "
                        + paymentRequest.getAmount() + " F CFA",
                "payroll_paid");
        reportValidations.deleteById(request.paidDate());
        return payrollRequestResponse(paymentRequest);
    }

    private PayrollDueResponse dueFor(Worker worker, YearMonth through) {
        LocalDate throughDate = through.atEndOfMonth();
        LocalDate fromDate = payments
                .findFirstByWorkerIdAndPaidThroughEndIsNotNullOrderByPaidThroughEndDesc(worker.getId())
                .map(payment -> payment.getPaidThroughEnd().plusDays(1))
                .orElseGet(() -> attendance.findFirstByWorkerIdOrderByDateAsc(worker.getId())
                        .map(AttendanceRecord::getDate)
                        .orElse(null));
        if (fromDate == null || fromDate.isAfter(throughDate)) {
            return new PayrollDueResponse(worker.getId(), 0, 0, null, throughDate);
        }

        PayrollSettings payrollSettings = settings.findById(PayrollSettings.SINGLETON_ID)
                .orElseGet(() -> settings.save(new PayrollSettings(90_000, 208)));
        int monthlyRequiredHours = worker.getMonthlyRequiredHours() == null
                ? payrollSettings.getMonthlyRequiredHours()
                : worker.getMonthlyRequiredHours();
        long monthlyMinutes = (long) monthlyRequiredHours * 60;
        if (monthlyMinutes <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Monthly required hours are not configured.");
        }

        long totalMinutes = 0;
        long totalAmount = 0;
        YearMonth month = YearMonth.from(fromDate);
        while (!month.isAfter(through)) {
            LocalDate start = month.equals(YearMonth.from(fromDate)) ? fromDate : month.atDay(1);
            List<AttendanceRecord> records = attendance.findByWorkerIdAndDateBetween(
                    worker.getId(), start, month.atEndOfMonth());
            long minutes = records.stream()
                    .filter(record -> record.getStatus() == AttendanceRecord.Status.PRESENT)
                    .mapToLong(AttendanceRecord::workedMinutes)
                    .sum();
            totalMinutes += minutes;
            totalAmount += Math.round((double) worker.getSalary() * minutes / monthlyMinutes);
            month = month.plusMonths(1);
        }
        return new PayrollDueResponse(worker.getId(), totalAmount, totalMinutes, fromDate, throughDate);
    }

    private PayrollPaymentRequestResponse payrollRequestResponse(PayrollPaymentRequest request) {
        return new PayrollPaymentRequestResponse(
                request.getId(),
                request.getWorker().getId(),
                request.getWorker().getName(),
                request.getAmount(),
                request.getWorkedMinutes(),
                request.getFromDate(),
                request.getThroughDate(),
                request.getScheduledPaymentDate(),
                request.getStatus().name(),
                request.getRequestedAt(),
                request.getRequestedBy(),
                request.getDecidedAt(),
                request.getDecidedBy(),
                request.getPaidAt(),
                request.getPaidBy());
    }
}
