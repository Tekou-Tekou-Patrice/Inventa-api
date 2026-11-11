package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import com.onescompany.Inventa.domain.AttendanceRecord;
import com.onescompany.Inventa.domain.AttendanceRepository;
import com.onescompany.Inventa.domain.BudgetLedgerService;
import com.onescompany.Inventa.domain.DailyReportValidation;
import com.onescompany.Inventa.domain.DailyReportValidationRepository;
import com.onescompany.Inventa.domain.MaterialAlert;
import com.onescompany.Inventa.domain.MaterialAlertRepository;
import com.onescompany.Inventa.domain.Expense;
import com.onescompany.Inventa.domain.ExpenseRepository;
import com.onescompany.Inventa.domain.EvidenceAttachment;
import com.onescompany.Inventa.domain.EvidenceAttachmentRepository;
import com.onescompany.Inventa.domain.HarvestSale;
import com.onescompany.Inventa.domain.HarvestSaleRepository;
import com.onescompany.Inventa.domain.FieldTask;
import com.onescompany.Inventa.domain.FieldTaskRepository;
import com.onescompany.Inventa.domain.FieldTaskProgress;
import com.onescompany.Inventa.domain.FieldTaskProgressRepository;
import com.onescompany.Inventa.domain.FieldTaskAdvance;
import com.onescompany.Inventa.domain.FieldTaskAdvanceRepository;
import com.onescompany.Inventa.domain.OvertimeRecord;
import com.onescompany.Inventa.domain.OvertimeRepository;
import com.onescompany.Inventa.domain.Payment;
import com.onescompany.Inventa.domain.PaymentRepository;
import com.onescompany.Inventa.domain.PlantationField;
import com.onescompany.Inventa.domain.PlantationFieldRepository;
import com.onescompany.Inventa.domain.StockItem;
import com.onescompany.Inventa.domain.StockItemRepository;
import com.onescompany.Inventa.domain.StockItemPriceChange;
import com.onescompany.Inventa.domain.StockItemPriceChangeRepository;
import com.onescompany.Inventa.domain.StockOrder;
import com.onescompany.Inventa.domain.StockOrderRepository;
import com.onescompany.Inventa.domain.StockMovement;
import com.onescompany.Inventa.domain.StockMovementRepository;
import com.onescompany.Inventa.domain.Worker;
import com.onescompany.Inventa.domain.WorkerRepository;
import com.onescompany.Inventa.domain.SeasonalWorker;
import com.onescompany.Inventa.domain.SeasonalWorkerRepository;
import com.onescompany.Inventa.domain.ValidatedFieldTask;
import com.onescompany.Inventa.domain.ValidatedFieldTaskRepository;
import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
import com.onescompany.Inventa.security.EvidenceStorageService;
import com.onescompany.Inventa.security.EvidenceStorageService.StoredFile;
import com.onescompany.Inventa.security.PushNotificationService;
import com.onescompany.Inventa.security.UserRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Transactional
public class PlantationController {
    private final WorkerRepository workers;
    private final AttendanceRepository attendance;
    private final FieldTaskRepository tasks;
    private final StockItemRepository stockItems;
    private final StockMovementRepository stockMovements;
    private final StockOrderRepository stockOrders;
    private final MaterialAlertRepository materialAlerts;
    private final ExpenseRepository expenses;
    private final HarvestSaleRepository harvestSales;
    private final PaymentRepository payments;
    private final OvertimeRepository overtime;
    private final AppUserRepository users;
    private final PlantationFieldRepository fields;
    private final DailyReportValidationRepository reportValidations;
    private final ValidatedFieldTaskRepository validatedTasks;
    private final EvidenceAttachmentRepository evidenceAttachments;
    private final EvidenceStorageService evidenceStorage;
    private final BudgetLedgerService budgetLedger;
    private final StockItemPriceChangeRepository stockItemPriceChanges;
    private final PushNotificationService pushNotifications;
    private final SeasonalWorkerRepository seasonalWorkers;
    private final FieldTaskProgressRepository taskProgress;
    private final FieldTaskAdvanceRepository taskAdvances;

    public PlantationController(
            WorkerRepository workers,
            AttendanceRepository attendance,
            FieldTaskRepository tasks,
            StockItemRepository stockItems,
            StockMovementRepository stockMovements,
            StockOrderRepository stockOrders,
            MaterialAlertRepository materialAlerts,
            ExpenseRepository expenses,
            HarvestSaleRepository harvestSales,
            PaymentRepository payments,
            OvertimeRepository overtime,
            AppUserRepository users,
            PlantationFieldRepository fields,
            DailyReportValidationRepository reportValidations,
            ValidatedFieldTaskRepository validatedTasks,
            EvidenceAttachmentRepository evidenceAttachments,
            EvidenceStorageService evidenceStorage,
            BudgetLedgerService budgetLedger,
            StockItemPriceChangeRepository stockItemPriceChanges,
            PushNotificationService pushNotifications,
            SeasonalWorkerRepository seasonalWorkers,
            FieldTaskProgressRepository taskProgress,
            FieldTaskAdvanceRepository taskAdvances) {
        this.workers = workers;
        this.attendance = attendance;
        this.tasks = tasks;
        this.stockItems = stockItems;
        this.stockMovements = stockMovements;
        this.stockOrders = stockOrders;
        this.materialAlerts = materialAlerts;
        this.expenses = expenses;
        this.harvestSales = harvestSales;
        this.payments = payments;
        this.overtime = overtime;
        this.users = users;
        this.fields = fields;
        this.reportValidations = reportValidations;
        this.validatedTasks = validatedTasks;
        this.evidenceAttachments = evidenceAttachments;
        this.evidenceStorage = evidenceStorage;
        this.budgetLedger = budgetLedger;
        this.stockItemPriceChanges = stockItemPriceChanges;
        this.pushNotifications = pushNotifications;
        this.seasonalWorkers = seasonalWorkers;
        this.taskProgress = taskProgress;
        this.taskAdvances = taskAdvances;
    }

    private boolean canViewWorkerSalary(AppUser account) {
        return account.getRole() == UserRole.DIRECTOR || account.getRole() == UserRole.ASSISTANT;
    }

    private boolean canViewStockPrices(AppUser account) {
        return account.getRole() == UserRole.DIRECTOR || account.getRole() == UserRole.ASSISTANT;
    }

    @GetMapping("/workers")
    public List<WorkerResponse> workers(Principal principal) {
        AppUser account = currentAccount(principal);
        return workers.findAll().stream()
                .filter(worker -> visible(worker.getField(), account))
                .map(worker -> workerResponse(worker, account))
                .toList();
    }

    @PostMapping("/workers")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public WorkerResponse createWorker(@Valid @RequestBody WorkerRequest request, Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = workers.save(new Worker(
                request.name().trim(),
                blankToDefault(request.groupName(), "Équipe terrain"),
                request.salary(),
                request.phoneNumber().trim(),
                managerField(account)));
        return workerResponse(worker, account);
    }

    @PutMapping("/workers/{id}/salary")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public WorkerResponse updateSalary(
            @PathVariable Long id,
            @Valid @RequestBody SalaryRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = requireWorker(id, account);
        if (account.getRole() != UserRole.DIRECTOR &&
                users.findByPayrollWorkerId(id).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the director may change a field manager's base salary.");
        }
        worker.updateSalary(request.salary());
        return workerResponse(worker, account);
    }

    @PutMapping("/workers/{id}/contact")
    @PreAuthorize("hasRole('ASSISTANT')")
    public WorkerResponse updateWorkerPhone(
            @PathVariable Long id,
            @Valid @RequestBody WorkerPhoneRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = requireWorker(id, account);
        worker.updatePhoneNumber(request.phoneNumber().trim());
        return workerResponse(worker, account);
    }

    @DeleteMapping("/workers/{id}")
    @PreAuthorize("hasRole('ASSISTANT')")
    public WorkerResponse archiveWorker(@PathVariable Long id, Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = requireWorker(id, account);
        worker.updateActive(false);
        return workerResponse(worker, account);
    }

    @PutMapping("/workers/{id}/active")
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public WorkerResponse updateWorkerActive(
            @PathVariable Long id,
            @Valid @RequestBody WorkerActiveRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = requireWorker(id, account);
        worker.updateActive(request.active());
        return workerResponse(worker, account);
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<AttendanceResponse> attendanceForDate(
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long fieldId,
            Principal principal) {
        AppUser account = currentAccount(principal);
        return attendance.findByDate(date).stream()
                .filter(record -> attendanceVisible(record.getWorker(), account, fieldId))
                .map(this::attendanceResponse)
                .toList();
    }

    @GetMapping("/attendance/team")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public AttendanceTeamResponse attendanceTeam(@RequestParam LocalDate date) {
        List<PlantationField> activeFields = fields.findAll().stream()
                .filter(PlantationField::isActive)
                .toList();
        Set<Long> fieldIds = activeFields.stream().map(PlantationField::getId).collect(Collectors.toSet());
        List<AttendanceFieldResponse> fieldOptions = activeFields.stream()
                .map(field -> new AttendanceFieldResponse(field.getId(), field.getName()))
                .toList();
        List<AttendanceWorkerResponse> team = workers.findAll().stream()
                .filter(Worker::isActive)
                .filter(worker -> worker.getField() != null && fieldIds.contains(worker.getField().getId()))
                .map(worker -> new AttendanceWorkerResponse(
                        worker.getId(), worker.getName(), worker.getGroupName(), worker.isActive(),
                        fieldId(worker.getField()), fieldName(worker.getField())))
                .toList();
        List<AttendanceResponse> records = attendance.findByDate(date).stream()
                .filter(record -> record.getWorker().getField() != null
                        && fieldIds.contains(record.getWorker().getField().getId()))
                .map(this::attendanceResponse)
                .toList();
        return new AttendanceTeamResponse(fieldOptions, team, records);
    }

    @GetMapping("/attendance/month")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<AttendanceResponse> attendanceForMonth(@RequestParam YearMonth month, Principal principal) {
        AppUser account = currentAccount(principal);
        return attendance.findByDateBetween(month.atDay(1), month.atEndOfMonth()).stream()
                .filter(record -> visible(record.getWorker().getField(), account))
                .map(this::attendanceResponse)
                .toList();
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public DailyReportResponse dailyReport(@RequestParam LocalDate date, Principal principal) {
        AppUser account = currentAccount(principal);
        DailyReportValidation validation = reportValidations.findById(date).orElse(null);
        if (account.getRole() == UserRole.DIRECTOR && validation == null) {
            return new DailyReportResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    false, null, null);
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        return new DailyReportResponse(
                attendance.findByDate(date).stream()
                        .filter(record -> visible(record.getWorker().getField(), account))
                        .map(this::attendanceResponse)
                        .toList(),
                tasks.findByCreatedAtBetween(start, end).stream()
                        .filter(task -> visible(task.getField(), account))
                        .map(this::taskResponse)
                        .toList(),
                stockMovements
                        .findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                        .stream()
                        .filter(movement -> visible(movement.getItem().getField(), account))
                        .map(movement -> stockMovementResponse(movement, account))
                        .toList(),
                materialAlerts
                        .findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                        .stream()
                        .filter(alert -> visible(alert.getItem().getField(), account))
                        .filter(alert -> account.getRole() != UserRole.DIRECTOR || alert.isResolved())
                        .map(this::materialAlertResponse)
                        .toList(),
                expenses.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                        .stream()
                        .filter(expense -> visible(expense.getField(), account))
                        .filter(expense -> !expense.isBudgeted()
                                || account.getRole() != UserRole.FIELD_MANAGER)
                        .map(this::expenseResponse)
                        .toList(),
                payments.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                        .stream()
                        .filter(payment -> visible(payment.getWorker().getField(), account))
                        .map(this::paymentResponse)
                        .toList(),
                harvestSales
                        .findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                        .stream()
                        .filter(sale -> visible(sale.getField(), account))
                        .map(this::harvestSaleResponse)
                        .toList(),
                validation != null,
                validation == null ? null : validation.getValidatedAt(),
                validation == null ? null : validation.getValidatedBy());
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public MonthlyReportResponse monthlyReport(@RequestParam YearMonth month, Principal principal) {
        AppUser account = currentAccount(principal);
        ZoneId zone = ZoneId.systemDefault();
        Instant start = month.atDay(1).atStartOfDay(zone).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        List<LocalDate> dates = month.atDay(1).datesUntil(month.plusMonths(1).atDay(1)).toList();
        List<LocalDate> validatedDates = reportValidations.findAllById(dates).stream()
                .map(DailyReportValidation::getReportDate)
                .toList();
        Set<LocalDate> visibleDates = account.getRole() == UserRole.DIRECTOR
                ? Set.copyOf(validatedDates)
                : Set.copyOf(dates);
        return new MonthlyReportResponse(
                validatedDates,
                attendance.findByDateBetween(month.atDay(1), month.atEndOfMonth()).stream()
                       .filter(record -> visible(record.getWorker().getField(), account))
                       .filter(record -> visibleDates.contains(record.getDate()))
                       .map(this::attendanceResponse)
                       .toList(),
                tasks.findByCreatedAtBetween(start, end.minusNanos(1)).stream()
                       .filter(task -> visible(task.getField(), account))
                       .filter(task -> visibleDates.contains(task.getCreatedAt().atZone(zone).toLocalDate()))
                       .map(this::taskResponse)
                       .toList(),
                stockMovements
                       .findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                       .stream()
                       .filter(movement -> visible(movement.getItem().getField(), account))
                       .filter(movement -> visibleDates.contains(movement.getRecordedAt().atZone(zone).toLocalDate()))
                       .map(movement -> stockMovementResponse(movement, account))
                       .toList(),
                materialAlerts
                       .findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                       .stream()
                       .filter(alert -> visible(alert.getItem().getField(), account))
                       .filter(alert -> account.getRole() != UserRole.DIRECTOR || alert.isResolved())
                       .filter(alert -> visibleDates.contains(alert.getRecordedAt().atZone(zone).toLocalDate()))
                       .map(this::materialAlertResponse)
                       .toList(),
                expenses.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                       .stream()
                       .filter(expense -> visible(expense.getField(), account))
                       .filter(expense -> !expense.isBudgeted()
                               || account.getRole() != UserRole.FIELD_MANAGER)
                       .filter(expense -> visibleDates.contains(expense.getRecordedAt().atZone(zone).toLocalDate()))
                       .map(this::expenseResponse)
                       .toList(),
                payments.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                       .stream()
                       .filter(payment -> visible(payment.getWorker().getField(), account))
                       .map(this::paymentResponse)
                       .toList(),
                harvestSales
                       .findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                       .stream()
                       .filter(sale -> visible(sale.getField(), account))
                       .filter(sale -> visibleDates.contains(sale.getRecordedAt().atZone(zone).toLocalDate()))
                       .map(this::harvestSaleResponse)
                       .toList(),
                overtime.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                       .stream()
                       .filter(record -> visible(record.getWorker().getField(), account))
                       .filter(record -> visibleDates.contains(record.getRecordedAt().atZone(zone).toLocalDate()))
                       .map(this::overtimeResponse)
                       .toList());
    }

    @PutMapping("/reports/daily/validate")
    @PreAuthorize("hasRole('ASSISTANT')")
    public DailyReportValidationResponse validateDailyReport(
            @RequestParam LocalDate date,
            Principal principal) {
        DailyReportValidation validation = reportValidations.findById(date)
                .orElseGet(() -> reportValidations.save(new DailyReportValidation(date, principal.getName())));
        snapshotTasksForReport(date, validation.getValidatedBy(), validation.getValidatedAt());
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Rapport reçu",
                "Le rapport du " + date + " a été validé par " + principal.getName() + ".",
                "daily_report_validated");
        return new DailyReportValidationResponse(
                validation.getReportDate(), true, validation.getValidatedAt(), validation.getValidatedBy());
    }

    @PutMapping("/attendance/{workerId}")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public AttendanceResponse markAttendance(
            @PathVariable Long workerId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long fieldId,
            @Valid @RequestBody AttendanceRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = requireAttendanceWorker(workerId, account, fieldId);
        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Past attendance dates are read-only.");
        }
        AttendanceRecord.Status status = AttendanceRecord.Status.valueOf(request.status().name());
        AttendanceRecord existing = attendance.findByWorkerIdAndDate(workerId, date).orElse(null);
        if (existing != null && existing.closeExpiredSession(Instant.now(), "Automatique")) {
            reportValidations.deleteById(date);
        }
        AttendanceRecord record;
        if (status == AttendanceRecord.Status.PRESENT && request.checkOut() != null) {
            Instant checkIn = request.checkIn();
            if (checkIn != null && request.checkOut().isBefore(checkIn)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Departure cannot be earlier than arrival.");
            }
            if (checkIn != null) {
                if (existing == null) {
                    record = new AttendanceRecord(
                            worker, date, status, checkIn, request.checkOut(), principal.getName());
                } else {
                    existing.update(status, checkIn, request.checkOut(), principal.getName());
                    record = existing;
                }
            } else if (existing == null
                    || !existing.closeSession(request.checkOut(), principal.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "There is no open attendance session to close.");
            } else {
                record = existing;
            }
        } else if (status == AttendanceRecord.Status.PRESENT) {
            Instant checkIn = request.checkIn() == null ? Instant.now() : request.checkIn();
            if (existing == null) {
                record = new AttendanceRecord(
                        worker, date, status, checkIn, null, principal.getName());
            } else if (existing.getStatus() != AttendanceRecord.Status.PRESENT) {
                existing.update(status, checkIn, null, principal.getName());
                record = existing;
            } else {
                if (existing.getCheckOut() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "The attendance session is already open.");
                }
                existing.startSession(checkIn, principal.getName());
                record = existing;
            }
        } else {
            if (existing != null) {
                existing.update(status, null, null, principal.getName());
                record = existing;
            } else {
                record = new AttendanceRecord(worker, date, status, null, null, principal.getName());
            }
        }
        AttendanceRecord saved = attendance.save(record);
        reportValidations.deleteById(date);
        return attendanceResponse(saved);
    }

    @PostMapping("/attendance/self/check-in")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public AttendanceResponse checkInFieldManager(Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = managerPayrollWorker(account);
        LocalDate date = LocalDate.now();
        AttendanceRecord record = attendance.findByWorkerIdAndDate(worker.getId(), date).orElse(null);
        if (record != null && record.closeExpiredSession(Instant.now(), "Automatique")) {
            reportValidations.deleteById(date);
        }
        if (record != null && record.getCheckOut() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Today's arrival is already recorded.");
        }
        AttendanceRecord saved;
        if (record == null) {
            saved = attendance.save(new AttendanceRecord(
                    worker, date, AttendanceRecord.Status.PRESENT, Instant.now(), null, principal.getName()));
        } else {
            record.startSession(Instant.now(), principal.getName());
            saved = attendance.save(record);
        }
        reportValidations.deleteById(date);
        return attendanceResponse(saved);
    }

    @PostMapping("/attendance/self/check-out")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public AttendanceResponse checkOutFieldManager(Principal principal) {
        AppUser account = currentAccount(principal);
        Worker worker = managerPayrollWorker(account);
        LocalDate date = LocalDate.now();
        AttendanceRecord record = attendance.findByWorkerIdAndDate(worker.getId(), date)
                .filter(value -> value.getStatus() == AttendanceRecord.Status.PRESENT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Record your arrival before departure."));
        record.closeExpiredSession(Instant.now(), "Automatique");
        if (record.getCheckOut() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Today's departure is already recorded.");
        }
        if (!record.closeSession(Instant.now(), principal.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Today's departure is already recorded.");
        }
        AttendanceRecord saved = attendance.save(record);
        reportValidations.deleteById(date);
        return attendanceResponse(saved);
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<TaskResponse> tasks(Principal principal) {
        AppUser account = currentAccount(principal);
        if (account.getRole() == UserRole.DIRECTOR) {
            backfillValidatedTaskSnapshots();
            return validatedTasks.findAllByOrderByValidatedAtAscIdAsc().stream()
                    .filter(task -> account.getField() == null
                            || task.getFieldId() == null
                            || task.getFieldId().equals(account.getField().getId()))
                    .map(this::taskResponse)
                    .toList();
        }
        List<FieldTask> visibleTasks = tasks.findAll().stream()
                .filter(task -> visible(task.getField(), account))
                .toList();
        return visibleTasks.stream()
                .map(this::taskResponse)
                .toList();
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAnyRole('FIELD_MANAGER','TECHNICAL_MANAGER')")
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request, Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = account.getRole() == UserRole.FIELD_MANAGER
                ? managerField(account)
                : targetField(request.fieldId(), account);
        if (account.getRole() == UserRole.TECHNICAL_MANAGER && request.fieldId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A field is required.");
        }
        FieldTask.Type type = request.type() == null
                ? FieldTask.Type.ASSIGNED_WORKERS
                : FieldTask.Type.valueOf(request.type().name());
        List<Long> requestedAssigneeIds = request.assigneeIds() == null
                ? List.of()
                : request.assigneeIds();
        List<Long> requestedSeasonalWorkerIds = request.seasonalWorkerIds() == null
                ? List.of()
                : request.seasonalWorkerIds();
        if ((type == FieldTask.Type.ASSIGNED_WORKERS && !requestedSeasonalWorkerIds.isEmpty())
                || (account.getRole() == UserRole.FIELD_MANAGER
                        && type == FieldTask.Type.ASSIGNED_WORKERS && requestedAssigneeIds.isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose workers matching the task type.");
        }
        Set<Long> ids = Set.copyOf(requestedAssigneeIds);
        Set<Worker> assignees = workers.findAllById(ids).stream()
                .filter(worker -> sameField(worker.getField(), field))
                .collect(Collectors.toSet());
        if (assignees.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more assigned workers do not exist.");
        }
        Set<Long> seasonalIds = Set.copyOf(requestedSeasonalWorkerIds);
        Set<SeasonalWorker> seasonalAssignees = seasonalWorkers.findAllById(seasonalIds).stream()
                .filter(SeasonalWorker::isActive)
                .filter(worker -> sameField(worker.getField(), field))
                .collect(Collectors.toSet());
        if (seasonalAssignees.size() != seasonalIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more seasonal workers do not exist.");
        }
        if (type == FieldTask.Type.SEASONAL_WORKERS
                && (request.plannedHectares() == null || request.pricePerHectare() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Seasonal tasks require planned hectares and a price per hectare.");
        }
        if (type == FieldTask.Type.ASSIGNED_WORKERS
                && (request.plannedHectares() != null || request.pricePerHectare() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hectare pricing is only valid for seasonal tasks.");
        }
        FieldTask.Priority priority;
        try {
            priority = request.priority() == null
                    ? FieldTask.Priority.NORMAL
                    : FieldTask.Priority.valueOf(request.priority());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task priority.");
        }
        FieldTask task = tasks.save(new FieldTask(
                request.title().trim(),
                request.description() == null || request.description().isBlank()
                        ? request.title().trim()
                        : request.description().trim(),
                priority,
                assignees,
                seasonalAssignees,
                principal.getName(),
                field,
                type,
                request.plannedHectares(),
                request.pricePerHectare()));
        pushNotifications.notifyRoleInField(
                UserRole.FIELD_MANAGER,
                field.getId(),
                "Nouvelle tâche",
                task.getTitle() + " · " + task.getDescription(),
                "field_task_created");
        invalidateDailyReport(LocalDate.now());
        return taskResponse(task);
    }

    @PutMapping("/tasks/{id}/status")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public TaskResponse updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        FieldTask task = tasks.findById(id)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (request.actualCost() != null || request.seasonalWorkerCount() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Task completion no longer accepts cost or seasonal headcount; report seasonal progress separately.");
        }
        task.updateStatus(
                FieldTask.Status.valueOf(request.status().name()),
                null,
                0);
        invalidateDailyReport(task.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return taskResponse(task);
    }

    @GetMapping("/seasonal-workers")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<SeasonalWorkerResponse> seasonalWorkers(
            @RequestParam(required = false) Long fieldId,
            Principal principal) {
        AppUser account = currentAccount(principal);
        List<SeasonalWorker> profiles;
        if (account.getRole() == UserRole.FIELD_MANAGER) {
            profiles = seasonalWorkers.findByFieldIdAndActiveTrueOrderByNameAsc(
                    managerField(account).getId());
        } else if (fieldId != null) {
            profiles = seasonalWorkers.findByFieldIdAndActiveTrueOrderByNameAsc(fieldId).stream()
                    .filter(worker -> visible(worker.getField(), account))
                    .toList();
        } else {
            profiles = seasonalWorkers.findAll().stream()
                    .filter(SeasonalWorker::isActive)
                    .filter(worker -> visible(worker.getField(), account))
                    .toList();
        }
        return profiles.stream().map(this::seasonalWorkerResponse).toList();
    }

    @PostMapping("/seasonal-workers")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public SeasonalWorkerResponse createSeasonalWorker(
            @Valid @RequestBody SeasonalWorkerRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = managerField(account);
        SeasonalWorker profile = seasonalWorkers.save(new SeasonalWorker(
                request.name().trim(),
                request.phoneNumber().trim(),
                field,
                principal.getName()));
        return seasonalWorkerResponse(profile);
    }

    @PutMapping("/seasonal-workers/{id}")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public SeasonalWorkerResponse updateSeasonalWorker(
            @PathVariable Long id,
            @Valid @RequestBody SeasonalWorkerRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = managerField(account);
        SeasonalWorker profile = seasonalWorkers.findById(id)
                .filter(SeasonalWorker::isActive)
                .filter(value -> sameField(value.getField(), field))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seasonal worker not found."));
        profile.updateContact(request.name().trim(), request.phoneNumber().trim());
        return seasonalWorkerResponse(profile);
    }

    @DeleteMapping("/seasonal-workers/{id}")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public void archiveSeasonalWorker(@PathVariable Long id, Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = managerField(account);
        SeasonalWorker profile = seasonalWorkers.findById(id)
                .filter(value -> sameField(value.getField(), field))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seasonal worker not found."));
        profile.archive();
    }

    @PutMapping("/tasks/{id}/seasonal-workers")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public TaskResponse assignSeasonalWorkers(
            @PathVariable Long id,
            @Valid @RequestBody SeasonalWorkerAssignmentRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        FieldTask task = tasks.findByIdForUpdate(id)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (task.getType() != FieldTask.Type.SEASONAL_WORKERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a seasonal task can have seasonal workers.");
        }
        Set<Long> ids = Set.copyOf(request.seasonalWorkerIds());
        Set<SeasonalWorker> assignments = seasonalWorkers.findAllById(ids).stream()
                .filter(SeasonalWorker::isActive)
                .filter(worker -> sameField(worker.getField(), task.getField()))
                .collect(Collectors.toSet());
        if (assignments.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more seasonal workers do not exist.");
        }
        task.assignSeasonalWorkers(assignments);
        return taskResponse(task);
    }

    @PostMapping("/tasks/{id}/progress")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public TaskProgressResponse updateTaskProgress(
            @PathVariable Long id,
            @Valid @RequestBody TaskProgressRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        FieldTask task = tasks.findByIdForUpdate(id)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (task.getType() != FieldTask.Type.SEASONAL_WORKERS
                || task.getPlannedHectares() == null
                || task.getPricePerHectare() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This task does not track seasonal hectares.");
        }
        var existingProgress = taskProgress.findByReportedByAndIdempotencyKey(
                principal.getName(), request.idempotencyKey());
        if (existingProgress.isPresent()) {
            if (!existingProgress.get().getTask().getId().equals(task.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Progress idempotency key was already used.");
            }
            return taskProgressResponse(existingProgress.get());
        }
        BigDecimal completed = request.completedHectares().stripTrailingZeros();
        if (completed.compareTo(BigDecimal.ZERO) < 0
                || completed.compareTo(task.getPlannedHectares()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed hectares are outside the planned range.");
        }
        long earned = earnedAmount(completed, task.getPricePerHectare());
        if (taskAdvances.sumAmountByTaskId(task.getId()) > earned) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Progress cannot be reduced below advances already paid.");
        }
        FieldTaskProgress saved = taskProgress.save(
                new FieldTaskProgress(
                        task, completed, earned, principal.getName(), request.idempotencyKey()));
        String notification = task.getTitle() + " · " + completed.toPlainString() + " ha · "
                + earned + " F acquis · reste "
                + remainingWorkCost(task, completed) + " F CFA.";
        pushNotifications.notifyRole(
                UserRole.ASSISTANT, "Avancement d’une tâche saisonnière", notification, "seasonal_task_progress");
        pushNotifications.notifyRole(
                UserRole.TECHNICAL_MANAGER, "Avancement d’une tâche saisonnière", notification, "seasonal_task_progress");
        invalidateDailyReport(task.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return taskProgressResponse(saved);
    }

    @PostMapping("/tasks/{id}/advances")
    @PreAuthorize("hasRole('ASSISTANT')")
    public TaskAdvanceResponse recordTaskAdvance(
            @PathVariable Long id,
            @Valid @RequestBody TaskAdvanceRequest request,
            Principal principal) {
        FieldTask task = tasks.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (task.getType() != FieldTask.Type.SEASONAL_WORKERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Advances are only available for seasonal tasks.");
        }
        boolean hasWorker = request.workerId() != null;
        boolean hasSeasonalWorker = request.seasonalWorkerId() != null;
        if (hasWorker == hasSeasonalWorker) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose exactly one advance recipient.");
        }
        var existing = taskAdvances.findByPaidByAndIdempotencyKey(
                principal.getName(), request.idempotencyKey());
        if (existing.isPresent()) {
            return taskAdvanceResponse(existing.get());
        }
        Worker worker = hasWorker
                ? workers.findById(request.workerId())
                        .filter(value -> task.getAssignees().stream()
                                .anyMatch(assigned -> assigned.getId().equals(value.getId())))
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "The worker is not assigned to this task."))
                : null;
        SeasonalWorker seasonalWorker = hasSeasonalWorker
                ? seasonalWorkers.findById(request.seasonalWorkerId())
                        .filter(value -> task.getSeasonalAssignees().stream()
                                .anyMatch(assigned -> assigned.getId().equals(value.getId())))
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "The seasonal worker is not assigned to this task."))
                : null;
        long earned = taskProgress.findFirstByTaskIdOrderByReportedAtDescIdDesc(task.getId())
                .map(FieldTaskProgress::getEarnedAmount)
                .orElse(0L);
        long alreadyPaid = taskAdvances.sumAmountByTaskId(task.getId());
        if (request.amount() > earned - alreadyPaid) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The advance exceeds the earned amount still available.");
        }
        FieldTaskAdvance advance = taskAdvances.save(new FieldTaskAdvance(
                task,
                worker,
                seasonalWorker,
                request.amount(),
                principal.getName(),
                request.idempotencyKey()));
        return taskAdvanceResponse(advance);
    }

    @PostMapping(value = "/tasks/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public List<EvidenceResponse> uploadTaskEvidence(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            Principal principal) {
        AppUser account = currentAccount(principal);
        FieldTask task = tasks.findById(id)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
        if (task.getStatus() != FieldTask.Status.DONE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Evidence can only be attached to a completed task.");
        }
        attachEvidence(
                EvidenceAttachment.EntityType.TASK,
                task.getId(),
                fieldId(task.getField()),
                files,
                principal.getName());
        return evidenceResponses(EvidenceAttachment.EntityType.TASK, task.getId());
    }

    @GetMapping("/stock/items")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<StockItemResponse> stockItems(Principal principal) {
        AppUser account = currentAccount(principal);
        return stockItems.findAll().stream()
                .filter(StockItem::isActive)
                .filter(item -> visible(item.getField(), account))
                .map(item -> stockItemResponse(item, account))
                .toList();
    }

    @GetMapping("/stock/items/{id}/price-history")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public List<StockItemPriceChangeResponse> stockItemPriceHistory(@PathVariable Long id) {
        StockItem item = stockItems.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        return stockItemPriceChanges.findByItem_IdOrderByChangedAtDesc(item.getId()).stream()
                .map(change -> new StockItemPriceChangeResponse(
                        change.getId(),
                        item.getId(),
                        change.getPreviousUnitPrice(),
                        change.getNewUnitPrice(),
                        change.getChangedAt(),
                        change.getChangedBy()))
                .toList();
    }

    @GetMapping("/budgets")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public BudgetSnapshotResponse budgets() {
        return budgetSnapshotResponse(budgetLedger.snapshot());
    }

    @GetMapping("/budgets/field")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public BudgetSnapshotResponse fieldBudget(Principal principal) {
        PlantationField field = managerField(currentAccount(principal));
        BudgetLedgerService.FieldSnapshot snapshot = budgetLedger.snapshotForField(field);
        long confirmed = snapshot.allocatedAmount()
                - snapshot.remainingAmount()
                - snapshot.pendingExpenses();
        return new BudgetSnapshotResponse(
                snapshot.allocatedAmount(),
                confirmed,
                snapshot.pendingExpenses(),
                snapshot.remainingAmount(),
                0,
                List.of(new FieldBudgetResponse(
                        snapshot.fieldId(),
                        snapshot.fieldName(),
                        snapshot.allocatedAmount(),
                        snapshot.remainingAmount(),
                        snapshot.pendingExpenses(),
                        snapshot.blocked())));
    }

    @PutMapping("/budgets/limit")
    @PreAuthorize("hasRole('DIRECTOR')")
    public BudgetSnapshotResponse setBudgetLimit(
            @Valid @RequestBody BudgetLimitRequest request,
            Principal principal) {
        budgetLedger.setFundingLimit(request.fundingLimit(), principal.getName());
        return budgetSnapshotResponse(budgetLedger.snapshot());
    }

    @PostMapping("/budgets/funding")
    @PreAuthorize("hasRole('DIRECTOR')")
    public BudgetSnapshotResponse addBudgetFunding(
            @Valid @RequestBody BudgetFundingRequest request,
            Principal principal) {
        budgetLedger.addFunding(
                request.amount(), principal.getName(), blankToDefault(request.note(), "Additional funding"));
        pushNotifications.notifyRole(
                UserRole.ASSISTANT,
                "Fonds ajoutés au budget",
                request.amount() + " F CFA ont été ajoutés au budget de fonctionnement.",
                "budget_funding_added");
        return budgetSnapshotResponse(budgetLedger.snapshot());
    }

    @PostMapping("/budgets/allocations")
    @PreAuthorize("hasRole('ASSISTANT')")
    public BudgetSnapshotResponse allocateBudget(
            @Valid @RequestBody BudgetAllocationRequest request,
            Principal principal) {
        PlantationField field = fields.findById(request.fieldId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        if (!field.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
        }
        budgetLedger.allocate(
                field.getId(), request.amount(), field, principal.getName(), blankToDefault(request.note(), ""));
        return budgetSnapshotResponse(budgetLedger.snapshot());
    }

    @PostMapping("/stock/items")
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public StockItemResponse createStockItem(
            @Valid @RequestBody StockItemRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = targetField(request.fieldId(), account);
        if (stockItems.existsByNameIgnoreCaseAndFieldId(request.name().trim(), field.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A stock item with that name already exists in this field.");
        }
        if (request.unitPrice() != null && account.getRole() != UserRole.ASSISTANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assistant can set a product price.");
        }
        StockItem item = new StockItem(request.name().trim(), request.unit().trim(), field);
        if (request.unitPrice() != null) {
            item.updateUnitPrice(request.unitPrice());
        }
        StockItem saved = stockItems.save(item);
        if (request.unitPrice() != null) {
            stockItemPriceChanges.save(new StockItemPriceChange(
                    saved, null, request.unitPrice(), principal.getName()));
        }
        return stockItemResponse(saved, account);
    }

    @PutMapping("/stock/items/{id}")
    @PreAuthorize("hasRole('ASSISTANT')")
    public StockItemResponse updateStockItem(
            @PathVariable Long id,
            @Valid @RequestBody StockItemUpdateRequest request,
            Principal principal) {
        StockItem item = stockMovements.findItemForUpdate(id)
                .filter(StockItem::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        String name = request.name().trim();
        String unit = request.unit().trim();
        if (!name.equalsIgnoreCase(item.getName())
                && stockItems.existsByNameIgnoreCaseAndFieldIdAndIdNot(
                        name, item.getField().getId(), item.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A stock item with that name already exists in this field.");
        }
        item.updateDetails(name, unit);
        return stockItemResponse(stockItems.save(item), currentAccount(principal));
    }

    @DeleteMapping("/stock/items/{id}")
    @PreAuthorize("hasRole('ASSISTANT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveStockItem(@PathVariable Long id) {
        StockItem item = stockMovements.findItemForUpdate(id)
                .filter(StockItem::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        if (item.getQuantity() != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A product with remaining stock cannot be deleted.");
        }
        if (stockOrders.existsByItem_IdAndStatusIn(
                id,
                List.of(
                        StockOrder.Status.PENDING_TECHNICAL_APPROVAL,
                        StockOrder.Status.ORDERED,
                        StockOrder.Status.TECHNICALLY_APPROVED,
                        StockOrder.Status.SENT))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A product with an in-progress order cannot be deleted.");
        }
        item.archive();
        stockItems.save(item);
    }

    @PutMapping("/stock/items/{id}/price")
    @PreAuthorize("hasRole('ASSISTANT')")
    public StockItemResponse updateStockItemPrice(
            @PathVariable Long id,
            @Valid @RequestBody StockItemPriceRequest request,
            Principal principal) {
        StockItem item = stockMovements.findItemForUpdate(id)
               .filter(StockItem::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        Long previousPrice = item.getUnitPrice();
        if (previousPrice == null || previousPrice.longValue() != request.unitPrice()) {
            item.updateUnitPrice(request.unitPrice());
            stockItems.save(item);
            stockItemPriceChanges.save(new StockItemPriceChange(
                    item, previousPrice, request.unitPrice(), principal.getName()));
        }
        return stockItemResponse(item, currentAccount(principal));
    }

    @GetMapping("/stock/movements")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<StockMovementResponse> stockMovements(Principal principal) {
        AppUser account = currentAccount(principal);
        return stockMovements.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(movement -> visible(movement.getItem().getField(), account))
                .map(movement -> stockMovementResponse(movement, account))
                .toList();
    }

    @GetMapping("/stock/orders")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER','TECHNICAL_MANAGER')")
    public List<StockOrderResponse> stockOrders(Principal principal) {
        AppUser account = currentAccount(principal);
        return stockOrders.findTop100ByOrderByOrderedAtDesc().stream()
                .filter(order -> visible(order.getField(), account))
                .filter(order -> account.getRole() != UserRole.DIRECTOR
                        || order.getStatus() == StockOrder.Status.RECEIVED)
                .map(order -> stockOrderResponse(order, account))
                .toList();
    }

    @PostMapping("/stock/orders")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public StockOrderResponse createStockOrder(
            @Valid @RequestBody StockOrderRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        StockItem item = stockMovements.findItemForUpdate(request.itemId())
                .filter(StockItem::isActive)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        StockOrder order = stockOrders.save(new StockOrder(
                item, request.quantity(), request.description().trim(), principal.getName()));
        pushNotifications.notifyRole(
                UserRole.TECHNICAL_MANAGER,
                "Commande à valider",
                item.getName() + " · " + request.quantity() + " " + item.getUnit()
                        + " · " + item.getField().getName(),
                "stock_technical_approval");
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Nouvelle demande de matériel",
                item.getName() + " · " + request.quantity() + " " + item.getUnit()
                        + " · " + item.getField().getName(),
                "stock_order_created");
        return stockOrderResponse(order, account);
    }

    @PutMapping("/stock/orders/{id}/technical-decision")
    @PreAuthorize("hasRole('TECHNICAL_MANAGER')")
    public StockOrderResponse decideStockOrder(
            @PathVariable Long id,
            @Valid @RequestBody StockOrderDecisionRequest request,
            Principal principal) {
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        try {
            order.decide(request.approved(), request.reason(), principal.getName());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        pushNotifications.notifyRole(
                UserRole.ASSISTANT,
                request.approved() ? "Commande approuvée" : "Commande refusée",
                order.getItem().getName() + (request.approved()
                        ? " peut maintenant être achetée."
                        : " : " + order.getTechnicalRejectionReason()),
                "stock_technical_decision");
        pushNotifications.notifyUsername(
                order.getOrderedBy(),
                request.approved() ? "Demande de matériel approuvée" : "Demande de matériel refusée",
                order.getItem().getName() + " · " + order.getField().getName()
                        + (request.approved() ? " : demande validée." : " : "
                                + order.getTechnicalRejectionReason()),
                "stock_request_decision");
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                request.approved() ? "Demande de matériel approuvée" : "Demande de matériel refusée",
                order.getItem().getName() + " · " + order.getField().getName(),
                "stock_technical_decision");
        return stockOrderResponse(order, currentAccount(principal));
    }

    @PutMapping("/stock/orders/{id}")
    @PreAuthorize("hasAnyRole('FIELD_MANAGER','TECHNICAL_MANAGER')")
    public StockOrderResponse updateStockOrder(
            @PathVariable Long id,
            @Valid @RequestBody StockOrderRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        boolean requester = principal.getName().equals(order.getOrderedBy());
        if (account.getRole() == UserRole.FIELD_MANAGER && !requester) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found.");
        }
        if (order.getStatus() != StockOrder.Status.PENDING_TECHNICAL_APPROVAL
                && order.getStatus() != StockOrder.Status.ORDERED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only a pending stock request can be edited.");
        }
        StockItem item = stockMovements.findItemForUpdate(request.itemId())
                .filter(StockItem::isActive)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        try {
            order.updateRequest(item, request.quantity(), request.description().trim(), principal.getName());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return stockOrderResponse(order, account);
    }

    @DeleteMapping("/stock/orders/{id}")
    @PreAuthorize("hasAnyRole('FIELD_MANAGER','TECHNICAL_MANAGER')")
    public StockOrderResponse cancelStockOrder(@PathVariable Long id, Principal principal) {
        AppUser account = currentAccount(principal);
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        if (account.getRole() == UserRole.FIELD_MANAGER
                && !principal.getName().equals(order.getOrderedBy())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found.");
        }
        try {
            order.cancel(principal.getName());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return stockOrderResponse(order, account);
    }

    @PostMapping("/stock/orders/{id}/revisions")
    @PreAuthorize("hasAnyRole('FIELD_MANAGER','TECHNICAL_MANAGER')")
    public StockOrderResponse reviseStockOrder(
            @PathVariable Long id,
            @Valid @RequestBody StockOrderRequest request,
            Principal principal) {
        StockOrder original = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        AppUser account = currentAccount(principal);
        if (account.getRole() == UserRole.FIELD_MANAGER
                && !principal.getName().equals(original.getOrderedBy())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found.");
        }
        if (original.getStatus() == StockOrder.Status.PENDING_TECHNICAL_APPROVAL
                || original.getStatus() == StockOrder.Status.ORDERED
                || original.getStatus() == StockOrder.Status.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only a decided request can be revised.");
        }
        StockItem item = stockMovements.findItemForUpdate(request.itemId())
                .filter(StockItem::isActive)
                .filter(value -> visible(value.getField(), account))
                .filter(value -> value.getField().getId().equals(original.getField().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        StockOrder revision = stockOrders.save(new StockOrder(
                item,
                request.quantity(),
                request.description().trim(),
                principal.getName(),
                original));
        pushNotifications.notifyRole(
                UserRole.TECHNICAL_MANAGER,
                "Commande à valider",
                item.getName() + " · " + request.quantity() + " " + item.getUnit()
                        + " · " + original.getField().getName(),
                "stock_technical_approval");
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Nouvelle demande de matériel",
                item.getName() + " · " + request.quantity() + " " + item.getUnit()
                        + " · " + original.getField().getName(),
                "stock_order_created");
        return stockOrderResponse(revision, account);
    }

    @PutMapping("/stock/orders/{id}/send")
    @PreAuthorize("hasRole('ASSISTANT')")
    public StockOrderResponse dispatchStockOrder(
            @PathVariable Long id,
            @Valid @RequestBody StockOrderDispatchRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        if (order.getStatus() != StockOrder.Status.TECHNICALLY_APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Technical approval is required before purchasing this stock order.");
        }
        StockItem item = stockMovements.findItemForUpdate(order.getItem().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        if (!Double.isFinite(request.sentQuantity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The sent quantity is invalid.");
        }
        if (item.getUnitPrice() == null || item.getUnitPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Set a positive product unit price before dispatch.");
        }
        try {
            order.dispatch(request.sentQuantity(), item.getUnitPrice(), principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        pushNotifications.notifyRoleInField(
                UserRole.FIELD_MANAGER,
                item.getField().getId(),
                "Approvisionnement envoyé",
                request.sentQuantity() + " " + item.getUnit() + " de " + item.getName()
                        + " est en route vers " + item.getField().getName() + ".",
                "stock_dispatched");
        return stockOrderResponse(order, account);
    }

    @PutMapping("/stock/orders/{id}/receive")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public StockOrderResponse receiveStockOrder(
            @PathVariable Long id,
            Principal principal) {
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        AppUser account = currentAccount(principal);
        if (!visible(order.getField(), account)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found.");
        }
        if (order.getStatus() != StockOrder.Status.SENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only a prepared stock delivery can be confirmed as received.");
        }
        StockItem item = stockMovements.findItemForUpdate(order.getItem().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        item.adjustQuantity(order.getReceivedQuantity());
        stockItems.save(item);
        order.receive(principal.getName());
        stockMovements.save(new StockMovement(
                item,
                StockMovement.Kind.RECEIPT,
                order.getReceivedQuantity(),
                order.getDescription(),
                principal.getName(),
                order.getPaidAmount()));
        pushNotifications.notifyRole(
                UserRole.ASSISTANT,
                "Réception de matériel confirmée",
                order.getReceivedQuantity() + " " + item.getUnit() + " de "
                        + item.getName() + " reçu au " + item.getField().getName() + ".",
                "stock_received");
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Mouvement de stock - réception",
                order.getReceivedQuantity() + " " + item.getUnit() + " de "
                        + item.getName() + " reçu au " + item.getField().getName() + ".",
                "stock_received");
        invalidateDailyReport(LocalDate.now());
        return stockOrderResponse(order, account);
    }

    @GetMapping("/stock/alerts")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<MaterialAlertResponse> materialAlerts(Principal principal) {
        AppUser account = currentAccount(principal);
        return materialAlerts.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(alert -> visible(alert.getItem().getField(), account))
                .filter(alert -> account.getRole() != UserRole.DIRECTOR || alert.isResolved())
                .map(this::materialAlertResponse)
                .toList();
    }

    @PostMapping("/stock/alerts")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public MaterialAlertResponse reportMaterialAlert(
            @Valid @RequestBody MaterialAlertRequest request,
            Principal principal) {
        if (request.kind() == MaterialAlertKind.DELIVERY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stock orders must be created by the assistant.");
        }
        AppUser account = currentAccount(principal);
        StockItem item = stockMovements.findItemForUpdate(request.itemId())
                .filter(StockItem::isActive)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        MaterialAlert alert = materialAlerts.save(new MaterialAlert(
                item,
                MaterialAlert.Kind.valueOf(request.kind().name()),
                request.quantity(),
                request.description().trim(),
                principal.getName()));
        invalidateDailyReport(LocalDate.now());
        return materialAlertResponse(alert);
    }

    @PutMapping("/stock/alerts/{id}/resolve")
    @PreAuthorize("hasRole('ASSISTANT')")
    public MaterialAlertResponse resolveMaterialAlert(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MaterialAlertResolutionRequest request,
            Principal principal) {
        MaterialAlert alert = materialAlerts.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material alert not found."));
        if (alert.isResolved()) {
            return materialAlertResponse(alert);
        }
        if (alert.getKind() == MaterialAlert.Kind.DELIVERY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Confirm stock deliveries through their assistant-prepared request.");
        } else if (request != null && request.paidAmount() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A missing-material alert cannot include a purchase price.");
        }
        alert.resolve();
        invalidateDailyReport(alert.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        invalidateDailyReport(LocalDate.now());
        return materialAlertResponse(alert);
    }

    @PostMapping("/stock/movements")
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public StockMovementResponse recordStockMovement(
            @Valid @RequestBody StockMovementRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        StockMovement.Kind kind = StockMovement.Kind.valueOf(request.kind().name());
        if (kind == StockMovement.Kind.RECEIPT) {
            if (account.getRole() != UserRole.ASSISTANT) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Only the assistant can validate a stock receipt.");
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Create an assistant stock order and receive it through that order.");
        }
        if (kind == StockMovement.Kind.USAGE && account.getRole() != UserRole.FIELD_MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the field manager can report stock usage.");
        }
        StockItem item = stockMovements.findItemForUpdate(request.itemId())
                .filter(StockItem::isActive)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        if (kind == StockMovement.Kind.USAGE && request.paidAmount() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A stock usage cannot include a purchase price.");
        }
        if (kind == StockMovement.Kind.USAGE && request.quantity() > item.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for this movement.");
        }
        item.adjustQuantity(kind == StockMovement.Kind.RECEIPT ? request.quantity() : -request.quantity());
        stockItems.save(item);
        StockMovement movement = stockMovements.save(new StockMovement(
                item, kind, request.quantity(), request.description().trim(),
                principal.getName(), request.paidAmount()));
        if (kind == StockMovement.Kind.USAGE) {
            pushNotifications.notifyRole(
                    UserRole.ASSISTANT,
                    "Matériel utilisé - " + item.getField().getName(),
                    request.quantity() + " " + item.getUnit() + " de " + item.getName()
                            + " signalés par le responsable de terrain.",
                    "stock_usage");
            pushNotifications.notifyRole(
                    UserRole.DIRECTOR,
                    "Mouvement de stock - utilisation",
                    request.quantity() + " " + item.getUnit() + " de " + item.getName()
                            + " signalés au " + item.getField().getName() + ".",
                    "stock_usage");
        }
        invalidateDailyReport(LocalDate.now());
        return stockMovementResponse(movement, account);
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<ExpenseResponse> expenses(Principal principal) {
        AppUser account = currentAccount(principal);
        return expenses.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(expense -> visible(expense.getField(), account))
                .map(this::expenseResponse)
                .toList();
    }

    @PostMapping(value = "/expenses/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ASSISTANT')")
    public List<EvidenceResponse> uploadExpenseEvidence(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            Principal principal) {
        Expense expense = expenses.findById(id)
                .filter(value -> principal.getName().equals(value.getRecordedBy()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found."));
        attachEvidence(
                EvidenceAttachment.EntityType.EXPENSE,
                expense.getId(),
                fieldId(expense.getField()),
                files,
                principal.getName());
        return evidenceResponses(EvidenceAttachment.EntityType.EXPENSE, expense.getId());
    }

    @PostMapping("/expenses")
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public ExpenseResponse recordExpense(
            @Valid @RequestBody ExpenseRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = targetField(request.fieldId(), account);
        Expense expense;
        if (account.getRole() == UserRole.ASSISTANT) {
            if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An expense idempotency key is required.");
            }
            expense = budgetLedger.recordBudgetedExpense(
                    request.description().trim(),
                    request.amount(),
                    request.idempotencyKey(),
                    field,
                    principal.getName(),
                    request.occurredAt() == null ? Instant.now() : request.occurredAt());
            pushNotifications.notifyRole(
                        UserRole.DIRECTOR,
                        "Nouvelle dépense de champ",
                        field.getName() + " · " + request.description().trim() + " · "
                                + request.amount() + " F CFA",
                        "assistant_expense");
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Field managers must record expenses against their field budget.");
        }
        invalidateDailyReport(expense.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return expenseResponse(expense);
    }

    @PostMapping("/expenses/field-budget")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public ExpenseResponse recordFieldBudgetExpense(
            @Valid @RequestBody FieldBudgetExpenseRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = managerField(account);
        Expense expense = budgetLedger.recordBudgetedExpense(
                request.description().trim(),
                request.amount(),
                request.idempotencyKey(),
                field,
                principal.getName(),
                request.occurredAt() == null ? Instant.now() : request.occurredAt());
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Dépense effectuée par le responsable",
                field.getName() + " · " + request.description().trim() + " · "
                        + request.amount() + " F CFA",
                "field_manager_expense");
        invalidateDailyReport(expense.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return expenseResponse(expense);
    }

    @PostMapping("/expenses/offline-sync")
    @PreAuthorize("hasRole('ASSISTANT')")
    public ExpenseResponse synchronizeOfflineExpense(
            @Valid @RequestBody OfflineExpenseSyncRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = targetField(request.fieldId(), account);
        Expense expense = budgetLedger.synchronizeOfflineExpense(
                request.description().trim(),
                request.amount(),
                request.idempotencyKey(),
                field,
                principal.getName(),
                request.occurredAt());
        pushNotifications.notifyRole(
                UserRole.DIRECTOR,
                "Dépense hors ligne à rapprocher",
                field.getName() + " · " + request.description().trim() + " · "
                        + request.amount() + " F CFA",
                "offline_expense");
        invalidateDailyReport(expense.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return expenseResponse(expense);
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<HarvestSaleResponse> harvestSales(Principal principal) {
        AppUser account = currentAccount(principal);
        return harvestSales.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(sale -> visible(sale.getField(), account))
                .map(this::harvestSaleResponse)
                .toList();
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public HarvestSaleResponse recordHarvestSale(
            @Valid @RequestBody HarvestSaleRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = targetField(request.fieldId(), account);
        HarvestSale sale = harvestSales.save(new HarvestSale(
                request.product().trim(),
                request.quantity(),
                request.totalPrice(),
                principal.getName(),
                field));
        UserRole recipient = account.getRole() == UserRole.FIELD_MANAGER
                ? UserRole.ASSISTANT
                : UserRole.DIRECTOR;
        pushNotifications.notifyRole(
                recipient,
                "Nouvelle vente enregistrée",
                sale.getProduct() + " · " + sale.getQuantity() + " · "
                        + sale.getTotalPrice() + " F CFA · " + field.getName(),
                "harvest_sale_recorded");
        invalidateDailyReport(sale.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return harvestSaleResponse(sale);
    }

    @PostMapping(value = "/sales/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public List<EvidenceResponse> uploadSaleEvidence(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            Principal principal) {
        AppUser account = currentAccount(principal);
        HarvestSale sale = harvestSales.findById(id)
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found."));
        attachEvidence(
                EvidenceAttachment.EntityType.SALE,
                sale.getId(),
                fieldId(sale.getField()),
                files,
                principal.getName());
        return evidenceResponses(EvidenceAttachment.EntityType.SALE, sale.getId());
    }

    @GetMapping("/evidence/{id}/content")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public ResponseEntity<Resource> evidenceContent(@PathVariable Long id, Principal principal) {
        AppUser account = currentAccount(principal);
        EvidenceAttachment attachment = evidenceAttachments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found."));
        PlantationField field = attachment.getFieldId() == null
                ? null
                : fields.findById(attachment.getFieldId()).orElse(null);
        if (!visible(field, account)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found.");
        }
        var file = evidenceStorage.resolve(attachment.getStorageKey());
        if (!java.nio.file.Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence file not found.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachment.getContentType()));
        headers.setContentLength(file.toFile().length());
        headers.setContentDisposition(ContentDisposition.inline().filename(attachment.getFileName()).build());
        headers.setCacheControl("private, max-age=3600");
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public List<PaymentResponse> payments(Principal principal) {
        AppUser account = currentAccount(principal);
        return payments.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(payment -> visible(payment.getWorker().getField(), account))
                .map(this::paymentResponse)
                .toList();
    }

    @GetMapping("/payments/monthly")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public List<PaymentResponse> monthlyPayments(
            @RequestParam YearMonth month,
            Principal principal) {
        AppUser account = currentAccount(principal);
        ZoneId zone = ZoneId.systemDefault();
        Instant start = month.atDay(1).atStartOfDay(zone).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        return payments.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(start, end)
                .stream()
                .filter(payment -> visible(payment.getWorker().getField(), account))
                .map(this::paymentResponse)
                .toList();
    }

    @PostMapping("/payments")
    @PreAuthorize("denyAll()")
    public PaymentResponse recordPayment(
            @Valid @RequestBody PaymentRequest request,
            Principal principal) {
        Worker worker = requireWorker(request.workerId(), currentAccount(principal));
        return paymentResponse(payments.save(new Payment(worker, request.amount(), principal.getName())));
    }

    @GetMapping("/overtime")
    @PreAuthorize("hasAnyRole('DIRECTOR','FIELD_MANAGER')")
    public List<OvertimeResponse> overtime(Principal principal) {
        AppUser account = currentAccount(principal);
        return overtime.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(record -> visible(record.getWorker().getField(), account))
                .map(this::overtimeResponse)
                .toList();
    }

    @PostMapping("/overtime")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public OvertimeResponse recordOvertime(
            @Valid @RequestBody OvertimeRequest request,
            Principal principal) {
        Worker worker = requireWorker(request.workerId(), currentAccount(principal));
        return overtimeResponse(overtime.save(new OvertimeRecord(
                worker, request.hours(), request.description().trim(), principal.getName())));
    }

    private AppUser currentAccount(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
    }

    private void invalidateDailyReport(LocalDate date) {
        reportValidations.deleteById(date);
    }

    private void backfillValidatedTaskSnapshots() {
        for (DailyReportValidation validation : reportValidations.findAll()) {
            if (!validatedTasks.existsByReportDate(validation.getReportDate())) {
                snapshotTasksForReport(
                        validation.getReportDate(),
                        validation.getValidatedBy(),
                        validation.getValidatedAt());
            }
        }
    }

    private void snapshotTasksForReport(LocalDate date, String validatedBy, Instant validatedAt) {
        ZoneId zone = ZoneId.systemDefault();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<ValidatedFieldTask> snapshots = tasks.findByCreatedAtBetween(start, end).stream()
                .map(task -> new ValidatedFieldTask(
                        task,
                        date,
                        validatedBy,
                        validatedAt,
                        evidenceAttachments.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
                                        EvidenceAttachment.EntityType.TASK, task.getId())
                                .stream()
                                .map(EvidenceAttachment::getId)
                                .toList()))
                .toList();
        validatedTasks.saveAll(snapshots);
    }

    private void attachEvidence(
            EvidenceAttachment.EntityType entityType,
            Long entityId,
            Long fieldId,
            List<MultipartFile> files,
            String username) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one evidence file.");
        }
        long existing = evidenceAttachments.countByEntityTypeAndEntityId(entityType, entityId);
        if (existing + files.size() > EvidenceStorageService.MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A record can have at most five evidence files.");
        }
        files.forEach(evidenceStorage::validate);
        var storedKeys = new java.util.ArrayList<String>();
        try {
            for (MultipartFile file : files) {
                StoredFile stored = evidenceStorage.store(file);
                storedKeys.add(stored.storageKey());
                evidenceAttachments.save(new EvidenceAttachment(
                        entityType,
                        entityId,
                        fieldId,
                        stored.fileName(),
                        stored.contentType(),
                        stored.storageKey(),
                        username));
            }
        } catch (java.io.IOException | RuntimeException error) {
            for (String storageKey : storedKeys) {
                try {
                    evidenceStorage.delete(storageKey);
                } catch (java.io.IOException cleanupError) {
                    error.addSuppressed(cleanupError);
                }
            }
            if (error instanceof RuntimeException runtimeError) throw runtimeError;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store evidence.", error);
        }
    }

    private List<EvidenceResponse> evidenceResponses(EvidenceAttachment.EntityType entityType, Long entityId) {
        return evidenceAttachments.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(entityType, entityId).stream()
                .map(attachment -> new EvidenceResponse(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getContentType(),
                        "/api/evidence/" + attachment.getId() + "/content",
                        attachment.getCreatedAt()))
                .toList();
    }

    private PlantationField managerField(AppUser account) {
        if (account.getRole() != UserRole.FIELD_MANAGER
                || account.getField() == null
                || !account.getField().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has no assigned field.");
        }
        return account.getField();
    }

    private PlantationField targetField(Long requestedId, AppUser account) {
        if (account.getRole() == UserRole.FIELD_MANAGER) {
            PlantationField assigned = managerField(account);
            if (requestedId != null && !requestedId.equals(assigned.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
            }
            return assigned;
        }
        if (requestedId != null) {
            PlantationField field = fields.findById(requestedId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
            if (!field.isActive()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found.");
            }
            return field;
        }
        return fields.findAll().stream().filter(PlantationField::isActive).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No field is configured."));
    }

    private boolean visible(PlantationField field, AppUser account) {
        if (account.getRole() != UserRole.FIELD_MANAGER) {
            return true;
        }
        return field != null && account.getField() != null && sameField(field, account.getField());
    }

    private boolean attendanceVisible(Worker worker, AppUser account, Long requestedFieldId) {
        PlantationField field = worker.getField();
        if (field == null) {
            return false;
        }
        if (account.getRole() == UserRole.FIELD_MANAGER) {
            if (!field.isActive()) {
                return false;
            }
            Long targetFieldId = requestedFieldId == null
                    ? account.getField() == null ? null : account.getField().getId()
                    : requestedFieldId;
            return targetFieldId != null && targetFieldId.equals(field.getId());
        }
        return requestedFieldId == null || requestedFieldId.equals(field.getId());
    }

    private Worker requireAttendanceWorker(Long id, AppUser account, Long fieldId) {
        return workers.findById(id)
                .filter(Worker::isActive)
                .filter(worker -> attendanceVisible(worker, account, fieldId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));
    }

    private boolean sameField(PlantationField first, PlantationField second) {
        return first != null && second != null && first.getId().equals(second.getId());
    }

    private Worker requireWorker(Long id, AppUser account) {
        return workers.findById(id)
                .filter(worker -> visible(worker.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));
    }

    private Worker managerPayrollWorker(AppUser account) {
        if (account.getPayrollWorker() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This field-manager account has no payroll profile.");
        }
        return account.getPayrollWorker();
    }

    private WorkerResponse workerResponse(Worker worker, AppUser account) {
        return new WorkerResponse(worker.getId(), worker.getName(), worker.getGroupName(),
                canViewWorkerSalary(account) ? worker.getSalary() : null,
                worker.isActive(), canViewWorkerSalary(account) && worker.isSalaryIsIndividual(),
                worker.getPhoneNumber() == null ? "" : worker.getPhoneNumber(),
                fieldId(worker.getField()), fieldName(worker.getField()),
                worker.getMonthlyRequiredHours());
    }

    private HarvestSaleResponse harvestSaleResponse(HarvestSale sale) {
        return new HarvestSaleResponse(
                sale.getId(),
                sale.getProduct(),
                sale.getQuantity(),
                sale.getTotalPrice(),
                sale.getRecordedAt(),
                sale.getRecordedBy(),
                fieldId(sale.getField()),
                fieldName(sale.getField()),
                evidenceResponses(EvidenceAttachment.EntityType.SALE, sale.getId()));
    }

    private AttendanceResponse attendanceResponse(AttendanceRecord record) {
        if (record.closeExpiredSession(Instant.now(), "Automatique")) {
            reportValidations.deleteById(record.getDate());
        }
        return new AttendanceResponse(record.getWorker().getId(), record.getDate(),
                AttendanceStatus.valueOf(record.getStatus().name()), record.getCheckIn(), record.getCheckOut(),
                record.getRecordedBy(), fieldId(record.getWorker().getField()), fieldName(record.getWorker().getField()),
                record.getSessions().stream()
                        .map(session -> new AttendanceSessionResponse(session.getCheckIn(), session.getCheckOut()))
                        .toList());
    }

    private MaterialAlertResponse materialAlertResponse(MaterialAlert alert) {
        return new MaterialAlertResponse(
                alert.getId(),
                alert.getItem().getId(),
                MaterialAlertKind.valueOf(alert.getKind().name()),
                alert.getQuantity(),
                alert.getDescription(),
                alert.getRecordedAt(),
                alert.getRecordedBy(),
                alert.isResolved(),
                fieldId(alert.getItem().getField()),
                fieldName(alert.getItem().getField()));
    }

    private TaskResponse taskResponse(FieldTask task) {
        List<TaskProgressResponse> progressHistory = taskProgress
                .findByTaskIdOrderByReportedAtAscIdAsc(task.getId()).stream()
                .map(this::taskProgressResponse)
                .toList();
        BigDecimal completedHectares = taskProgress
                .findFirstByTaskIdOrderByReportedAtDescIdDesc(task.getId())
                .map(FieldTaskProgress::getCompletedHectares)
                .orElse(BigDecimal.ZERO);
        long earned = taskProgress
                .findFirstByTaskIdOrderByReportedAtDescIdDesc(task.getId())
                .map(FieldTaskProgress::getEarnedAmount)
                .orElse(0L);
        return new TaskResponse(task.getId(), task.getTitle(),
                task.getAssignees().stream().map(Worker::getId).toList(),
                task.getStatus().name(),
                task.getType() == null ? FieldTask.Type.ASSIGNED_WORKERS.name() : task.getType().name(),
                task.getCreatedAt(), task.getCompletedAt(),
                task.getActualCost(), task.getSeasonalWorkerCount(),
                task.getCreatedBy(), fieldId(task.getField()),
                fieldName(task.getField()),
                evidenceResponses(EvidenceAttachment.EntityType.TASK, task.getId()),
                task.getDescription(),
                task.getPriority().name(),
                task.getPlannedHectares(),
                task.getPricePerHectare(),
                completedHectares,
                earned,
                task.getPlannedHectares() == null || task.getPricePerHectare() == null
                        ? null
                        : remainingWorkCost(task, completedHectares),
                task.getSeasonalAssignees().stream().map(SeasonalWorker::getId).toList(),
                task.getSeasonalAssignees().stream().map(this::seasonalWorkerResponse).toList(),
                progressHistory,
                taskAdvances.findByTaskIdOrderByPaidAtAscIdAsc(task.getId()).stream()
                        .map(this::taskAdvanceResponse)
                        .toList());
    }

    private TaskResponse taskResponse(ValidatedFieldTask task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getAssigneeIds(),
                task.getStatus(),
                task.getType(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                task.getActualCost(),
                task.getSeasonalWorkerCount(),
                task.getCreatedBy(),
                task.getFieldId(),
                task.getFieldName(),
                task.getEvidenceIds().stream()
                        .map(evidenceAttachments::findById)
                        .flatMap(java.util.Optional::stream)
                        .map(attachment -> new EvidenceResponse(
                                attachment.getId(),
                                attachment.getFileName(),
                                attachment.getContentType(),
                                "/api/evidence/" + attachment.getId() + "/content",
                                attachment.getCreatedAt()))
                        .toList(),
                task.getTitle(),
                FieldTask.Priority.NORMAL.name(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private SeasonalWorkerResponse seasonalWorkerResponse(SeasonalWorker worker) {
        return new SeasonalWorkerResponse(
                worker.getId(),
                worker.getName(),
                worker.getPhoneNumber(),
                fieldId(worker.getField()),
                fieldName(worker.getField()));
    }

    private TaskProgressResponse taskProgressResponse(FieldTaskProgress progress) {
        return new TaskProgressResponse(
                progress.getId(),
                progress.getCompletedHectares(),
                progress.getEarnedAmount(),
                progress.getReportedBy(),
                progress.getReportedAt());
    }

    private TaskAdvanceResponse taskAdvanceResponse(FieldTaskAdvance advance) {
        Worker worker = advance.getWorker();
        SeasonalWorker seasonalWorker = advance.getSeasonalWorker();
        return new TaskAdvanceResponse(
                advance.getId(),
                worker == null ? null : worker.getId(),
                worker == null ? null : worker.getName(),
                seasonalWorker == null ? null : seasonalWorker.getId(),
                seasonalWorker == null ? null : seasonalWorker.getName(),
                advance.getAmount(),
                advance.getPaidBy(),
                advance.getPaidAt());
    }

    private long earnedAmount(BigDecimal hectares, long rate) {
        return hectares.multiply(BigDecimal.valueOf(rate))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private long remainingWorkCost(FieldTask task, BigDecimal completedHectares) {
        BigDecimal remaining = task.getPlannedHectares().subtract(completedHectares);
        return remaining.multiply(BigDecimal.valueOf(task.getPricePerHectare()))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private StockItemResponse stockItemResponse(StockItem item, AppUser account) {
        return new StockItemResponse(item.getId(), item.getName(), item.getUnit(), item.getQuantity(),
                canViewStockPrices(account) ? item.getUnitPrice() : null,
                fieldId(item.getField()), fieldName(item.getField()));
    }

    private StockMovementResponse stockMovementResponse(StockMovement movement, AppUser account) {
        return new StockMovementResponse(movement.getId(), movement.getItem().getId(),
                movement.getItem().getName(), MovementKind.valueOf(movement.getKind().name()),
                movement.getQuantity(), movement.getDescription(), movement.getRecordedAt(),
                movement.getRecordedBy(), canViewStockPrices(account) ? movement.getPaidAmount() : null,
                fieldId(movement.getItem().getField()),
                fieldName(movement.getItem().getField()));
    }

    private StockOrderResponse stockOrderResponse(StockOrder order, AppUser account) {
        boolean canViewPrices = canViewStockPrices(account);
        return new StockOrderResponse(
                order.getId(),
                order.getItem().getId(),
                order.getItem().getName(),
                order.getItem().getUnit(),
                order.getOrderedQuantity(),
                order.getReceivedQuantity(),
                order.getDescription(),
                order.getOrderedAt(),
                order.getOrderedBy(),
                order.getSentAt(),
                order.getSentBy(),
                order.getReceivedAt(),
                order.getReceivedBy(),
                canViewPrices ? order.getPaidAmount() : null,
                canViewPrices ? order.getUnitPriceAtDispatch() : null,
                order.getStatus().name(),
                fieldId(order.getField()),
                fieldName(order.getField()),
                order.getTechnicalDecisionAt(),
                order.getTechnicalDecisionBy(),
                order.getTechnicalRejectionReason(),
                order.getUpdatedBy(),
                order.getUpdatedAt(),
                order.getRevisedFromId(),
                order.getRevisionNumber());
    }

    private ExpenseResponse expenseResponse(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getDescription(), expense.getAmount(),
                expense.getRecordedAt(), expense.getOccurredAt(), expense.getRecordedBy(),
                fieldId(expense.getField()), fieldName(expense.getField()), expense.getIdempotencyKey(),
                expense.isBudgeted(),
                expense.getReconciliationStatus().name(),
                evidenceResponses(EvidenceAttachment.EntityType.EXPENSE, expense.getId()));
    }

    private BudgetSnapshotResponse budgetSnapshotResponse(BudgetLedgerService.Snapshot snapshot) {
        return new BudgetSnapshotResponse(
                snapshot.fundingLimit(),
                snapshot.confirmedExpenses(),
                snapshot.pendingExpenses(),
                snapshot.remainingAmount(),
                snapshot.unallocatedAmount(),
                snapshot.fields().stream()
                        .map(field -> new FieldBudgetResponse(
                                field.fieldId(),
                                field.fieldName(),
                                field.allocatedAmount(),
                                field.remainingAmount(),
                                field.pendingExpenses(),
                                field.blocked()))
                        .toList());
    }

    private PaymentResponse paymentResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getWorker().getId(), payment.getWorker().getName(),
                payment.getAmount(), payment.getRecordedAt(), payment.getRecordedBy(),
                fieldId(payment.getWorker().getField()), fieldName(payment.getWorker().getField()),
                payment.getPaidThroughEnd());
    }

    private OvertimeResponse overtimeResponse(OvertimeRecord record) {
        return new OvertimeResponse(record.getId(), record.getWorker().getId(), record.getWorker().getName(),
                record.getHours(), record.getDescription(), record.getRecordedAt(), record.getRecordedBy(),
                fieldId(record.getWorker().getField()), fieldName(record.getWorker().getField()));
    }

    private Long fieldId(PlantationField field) {
        return field == null ? null : field.getId();
    }

    private String fieldName(PlantationField field) {
        return field == null ? null : field.getName();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
