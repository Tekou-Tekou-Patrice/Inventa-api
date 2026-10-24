package com.onescompany.Inventa.api;

import static com.onescompany.Inventa.api.ApiDtos.*;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.domain.AttendanceRecord;
import com.onescompany.Inventa.domain.AttendanceRepository;
import com.onescompany.Inventa.domain.BudgetLedgerService;
import com.onescompany.Inventa.domain.DailyReportValidation;
import com.onescompany.Inventa.domain.DailyReportValidationRepository;
import com.onescompany.Inventa.domain.MaterialAlert;
import com.onescompany.Inventa.domain.MaterialAlertRepository;
import com.onescompany.Inventa.domain.Expense;
import com.onescompany.Inventa.domain.ExpenseRepository;
import com.onescompany.Inventa.domain.HarvestSale;
import com.onescompany.Inventa.domain.HarvestSaleRepository;
import com.onescompany.Inventa.domain.FieldTask;
import com.onescompany.Inventa.domain.FieldTaskRepository;
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
import com.onescompany.Inventa.security.AppUser;
import com.onescompany.Inventa.security.AppUserRepository;
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
    private final BudgetLedgerService budgetLedger;
    private final StockItemPriceChangeRepository stockItemPriceChanges;
    private final PushNotificationService pushNotifications;

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
            BudgetLedgerService budgetLedger,
            StockItemPriceChangeRepository stockItemPriceChanges,
            PushNotificationService pushNotifications) {
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
        this.budgetLedger = budgetLedger;
        this.stockItemPriceChanges = stockItemPriceChanges;
        this.pushNotifications = pushNotifications;
    }

    @GetMapping("/workers")
    public List<WorkerResponse> workers(Principal principal) {
        AppUser account = currentAccount(principal);
        return workers.findAll().stream()
                .filter(worker -> visible(worker.getField(), account))
                .map(this::workerResponse)
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
        return workerResponse(worker);
    }

    @PutMapping("/workers/{id}/salary")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT')")
    public WorkerResponse updateSalary(
            @PathVariable Long id,
            @Valid @RequestBody SalaryRequest request,
            Principal principal) {
        Worker worker = requireWorker(id, currentAccount(principal));
        worker.updateSalary(request.salary());
        return workerResponse(worker);
    }

    @PutMapping("/workers/{id}/contact")
    @PreAuthorize("hasRole('ASSISTANT')")
    public WorkerResponse updateWorkerPhone(
            @PathVariable Long id,
            @Valid @RequestBody WorkerPhoneRequest request,
            Principal principal) {
        Worker worker = requireWorker(id, currentAccount(principal));
        worker.updatePhoneNumber(request.phoneNumber().trim());
        return workerResponse(worker);
    }

    @DeleteMapping("/workers/{id}")
    @PreAuthorize("hasRole('ASSISTANT')")
    public WorkerResponse archiveWorker(@PathVariable Long id, Principal principal) {
        Worker worker = requireWorker(id, currentAccount(principal));
        worker.updateActive(false);
        return workerResponse(worker);
    }

    @PutMapping("/workers/{id}/active")
    @PreAuthorize("hasAnyRole('ASSISTANT','FIELD_MANAGER')")
    public WorkerResponse updateWorkerActive(
            @PathVariable Long id,
            @Valid @RequestBody WorkerActiveRequest request,
            Principal principal) {
        Worker worker = requireWorker(id, currentAccount(principal));
        worker.updateActive(request.active());
        return workerResponse(worker);
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<AttendanceResponse> attendanceForDate(@RequestParam LocalDate date, Principal principal) {
        AppUser account = currentAccount(principal);
        return attendance.findByDate(date).stream()
                .filter(record -> visible(record.getWorker().getField(), account))
                .map(this::attendanceResponse)
                .toList();
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
                        .map(this::stockMovementResponse)
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
                       .map(this::stockMovementResponse)
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
        return new DailyReportValidationResponse(
                validation.getReportDate(), true, validation.getValidatedAt(), validation.getValidatedBy());
    }

    @PutMapping("/attendance/{workerId}")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public AttendanceResponse markAttendance(
            @PathVariable Long workerId,
            @RequestParam LocalDate date,
            @Valid @RequestBody AttendanceRequest request,
            Principal principal) {
        Worker worker = requireWorker(workerId, currentAccount(principal));
        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Past attendance dates are read-only.");
        }
        AttendanceRecord.Status status = AttendanceRecord.Status.valueOf(request.status().name());
        AttendanceRecord existing = attendance.findByWorkerIdAndDate(workerId, date).orElse(null);
        Instant checkIn = null;
        Instant checkOut = null;
        if (status == AttendanceRecord.Status.PRESENT) {
            checkIn = request.checkIn() != null
                    ? request.checkIn()
                    : existing != null && existing.getCheckIn() != null
                            ? existing.getCheckIn()
                            : Instant.now();
            checkOut = request.checkOut() != null
                    ? request.checkOut()
                    : existing == null ? null : existing.getCheckOut();
            if (checkOut != null && checkOut.isBefore(checkIn)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Departure cannot be earlier than arrival.");
            }
        }
        AttendanceRecord record;
        if (existing != null) {
            existing.update(status, checkIn, checkOut, principal.getName());
            record = existing;
        } else {
            record = new AttendanceRecord(worker, date, status, checkIn, checkOut, principal.getName());
        }
        AttendanceRecord saved = attendance.save(record);
        reportValidations.deleteById(date);
        return attendanceResponse(saved);
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<TaskResponse> tasks(Principal principal) {
        AppUser account = currentAccount(principal);
        return tasks.findAll().stream()
                .filter(task -> visible(task.getField(), account))
                .map(this::taskResponse)
                .toList();
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request, Principal principal) {
        AppUser account = currentAccount(principal);
        PlantationField field = managerField(account);
        FieldTask.Type type = request.type() == null
                ? FieldTask.Type.ASSIGNED_WORKERS
                : FieldTask.Type.valueOf(request.type().name());
        List<Long> requestedAssigneeIds = request.assigneeIds() == null
                ? List.of()
                : request.assigneeIds();
        if ((type == FieldTask.Type.ASSIGNED_WORKERS && requestedAssigneeIds.isEmpty())
                || (type == FieldTask.Type.SEASONAL_WORKERS && !requestedAssigneeIds.isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose registered workers or seasonal workers for the task.");
        }
        Set<Long> ids = Set.copyOf(requestedAssigneeIds);
        Set<Worker> assignees = workers.findAllById(ids).stream()
                .filter(worker -> sameField(worker.getField(), field))
                .collect(Collectors.toSet());
        if (assignees.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more assigned workers do not exist.");
        }
        FieldTask task = tasks.save(new FieldTask(
                request.title().trim(), assignees, principal.getName(), field, type));
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
        if (request.status() == TaskStatus.DONE &&
                (request.actualCost() == null
                        || (task.getType() == FieldTask.Type.SEASONAL_WORKERS
                                && request.seasonalWorkerCount() == null))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Completion requires cost and seasonal worker count when applicable.");
        }
        Integer seasonalWorkerCount = task.getType() == FieldTask.Type.SEASONAL_WORKERS
                ? request.seasonalWorkerCount()
                : Integer.valueOf(0);
        task.updateStatus(
                FieldTask.Status.valueOf(request.status().name()),
                request.actualCost(),
                seasonalWorkerCount);
        invalidateDailyReport(task.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return taskResponse(task);
    }

    @GetMapping("/stock/items")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<StockItemResponse> stockItems(Principal principal) {
        AppUser account = currentAccount(principal);
        return stockItems.findAll().stream()
                .filter(item -> visible(item.getField(), account))
                .map(this::stockItemResponse)
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
        return budgetSnapshotResponse(budgetLedger.snapshot());
    }

    @PostMapping("/budgets/allocations")
    @PreAuthorize("hasRole('ASSISTANT')")
    public BudgetSnapshotResponse allocateBudget(
            @Valid @RequestBody BudgetAllocationRequest request,
            Principal principal) {
        PlantationField field = fields.findById(request.fieldId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
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
        return stockItemResponse(saved);
    }

    @PutMapping("/stock/items/{id}/price")
    @PreAuthorize("hasRole('ASSISTANT')")
    public StockItemResponse updateStockItemPrice(
            @PathVariable Long id,
            @Valid @RequestBody StockItemPriceRequest request,
            Principal principal) {
        StockItem item = stockMovements.findItemForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        Long previousPrice = item.getUnitPrice();
        if (previousPrice == null || previousPrice.longValue() != request.unitPrice()) {
            item.updateUnitPrice(request.unitPrice());
            stockItems.save(item);
            stockItemPriceChanges.save(new StockItemPriceChange(
                    item, previousPrice, request.unitPrice(), principal.getName()));
        }
        return stockItemResponse(item);
    }

    @GetMapping("/stock/movements")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<StockMovementResponse> stockMovements(Principal principal) {
        AppUser account = currentAccount(principal);
        return stockMovements.findTop100ByOrderByRecordedAtDesc().stream()
                .filter(movement -> visible(movement.getItem().getField(), account))
                .map(this::stockMovementResponse)
                .toList();
    }

    @GetMapping("/stock/orders")
    @PreAuthorize("hasAnyRole('DIRECTOR','ASSISTANT','FIELD_MANAGER')")
    public List<StockOrderResponse> stockOrders(Principal principal) {
        AppUser account = currentAccount(principal);
        return stockOrders.findTop100ByOrderByOrderedAtDesc().stream()
                .filter(order -> visible(order.getItem().getField(), account))
                .filter(order -> account.getRole() != UserRole.DIRECTOR
                        || order.getStatus() == StockOrder.Status.RECEIVED)
                .map(this::stockOrderResponse)
                .toList();
    }

    @PostMapping("/stock/orders")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public StockOrderResponse createStockOrder(
            @Valid @RequestBody StockOrderRequest request,
            Principal principal) {
        AppUser account = currentAccount(principal);
        StockItem item = stockMovements.findItemForUpdate(request.itemId())
                .filter(value -> visible(value.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock item not found."));
        StockOrder order = stockOrders.save(new StockOrder(
                item, request.quantity(), request.description().trim(), principal.getName()));
        pushNotifications.notifyRole(
                UserRole.ASSISTANT,
                "Nouvelle demande de matériel",
                item.getName() + " · " + request.quantity() + " " + item.getUnit()
                        + " · " + item.getField().getName(),
                "stock_request");
        return stockOrderResponse(order);
    }

    @PutMapping("/stock/orders/{id}/send")
    @PreAuthorize("hasRole('ASSISTANT')")
    public StockOrderResponse dispatchStockOrder(
            @PathVariable Long id,
            @Valid @RequestBody StockOrderDispatchRequest request,
            Principal principal) {
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        if (order.getStatus() != StockOrder.Status.ORDERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This stock request has already been prepared.");
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
        return stockOrderResponse(order);
    }

    @PutMapping("/stock/orders/{id}/receive")
    @PreAuthorize("hasRole('FIELD_MANAGER')")
    public StockOrderResponse receiveStockOrder(
            @PathVariable Long id,
            Principal principal) {
        StockOrder order = stockOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock order not found."));
        AppUser account = currentAccount(principal);
        if (!visible(order.getItem().getField(), account)) {
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
        invalidateDailyReport(LocalDate.now());
        return stockOrderResponse(order);
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
        invalidateDailyReport(LocalDate.now());
        return stockMovementResponse(movement);
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
        invalidateDailyReport(sale.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        return harvestSaleResponse(sale);
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

    private PlantationField managerField(AppUser account) {
        if (account.getRole() != UserRole.FIELD_MANAGER || account.getField() == null) {
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
            return fields.findById(requestedId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found."));
        }
        return fields.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No field is configured."));
    }

    private boolean visible(PlantationField field, AppUser account) {
        if (account.getRole() != UserRole.FIELD_MANAGER) {
            return true;
        }
        return field != null && account.getField() != null && sameField(field, account.getField());
    }

    private boolean sameField(PlantationField first, PlantationField second) {
        return first != null && second != null && first.getId().equals(second.getId());
    }

    private Worker requireWorker(Long id, AppUser account) {
        return workers.findById(id)
                .filter(worker -> visible(worker.getField(), account))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));
    }

    private WorkerResponse workerResponse(Worker worker) {
        return new WorkerResponse(worker.getId(), worker.getName(), worker.getGroupName(),
                worker.getSalary(), worker.isActive(), worker.isSalaryIsIndividual(),
                worker.getPhoneNumber() == null ? "" : worker.getPhoneNumber(),
                fieldId(worker.getField()), fieldName(worker.getField()));
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
                fieldName(sale.getField()));
    }

    private AttendanceResponse attendanceResponse(AttendanceRecord record) {
        return new AttendanceResponse(record.getWorker().getId(), record.getDate(),
                AttendanceStatus.valueOf(record.getStatus().name()), record.getCheckIn(), record.getCheckOut(),
                record.getRecordedBy(),
                fieldId(record.getWorker().getField()), fieldName(record.getWorker().getField()));
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
        return new TaskResponse(task.getId(), task.getTitle(),
                task.getAssignees().stream().map(Worker::getId).toList(),
                task.getStatus().name(),
                task.getType() == null ? FieldTask.Type.ASSIGNED_WORKERS.name() : task.getType().name(),
                task.getCreatedAt(), task.getCompletedAt(),
                task.getActualCost(), task.getSeasonalWorkerCount(),
                task.getCreatedBy(), fieldId(task.getField()),
                fieldName(task.getField()));
    }

    private StockItemResponse stockItemResponse(StockItem item) {
        return new StockItemResponse(item.getId(), item.getName(), item.getUnit(), item.getQuantity(),
                item.getUnitPrice(),
                fieldId(item.getField()), fieldName(item.getField()));
    }

    private StockMovementResponse stockMovementResponse(StockMovement movement) {
        return new StockMovementResponse(movement.getId(), movement.getItem().getId(),
                movement.getItem().getName(), MovementKind.valueOf(movement.getKind().name()),
                movement.getQuantity(), movement.getDescription(), movement.getRecordedAt(),
                movement.getRecordedBy(), movement.getPaidAmount(), fieldId(movement.getItem().getField()),
                fieldName(movement.getItem().getField()));
    }

    private StockOrderResponse stockOrderResponse(StockOrder order) {
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
                order.getPaidAmount(),
                order.getUnitPriceAtDispatch(),
                order.getStatus().name(),
                fieldId(order.getItem().getField()),
                fieldName(order.getItem().getField()));
    }

    private ExpenseResponse expenseResponse(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getDescription(), expense.getAmount(),
                expense.getRecordedAt(), expense.getOccurredAt(), expense.getRecordedBy(),
                fieldId(expense.getField()), fieldName(expense.getField()), expense.getIdempotencyKey(),
                expense.isBudgeted(),
                expense.getReconciliationStatus().name());
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
