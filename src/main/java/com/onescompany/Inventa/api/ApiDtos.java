package com.onescompany.Inventa.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.onescompany.Inventa.security.UserRole;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record WorkerRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 120) String groupName,
            @Positive long salary,
            Boolean salaryIsIndividual,
            @NotBlank @Size(max = 40) String phoneNumber) {
    }

    public record WorkerResponse(
            Long id, String name, String group, Long salary, boolean active,
            boolean salaryIsIndividual, String phoneNumber, Long fieldId, String fieldName,
            Integer monthlyRequiredHours) {
    }

    public record SalaryRequest(@Positive long salary, Boolean salaryIsIndividual) {
    }

    public record WorkerPhoneRequest(@NotNull @Size(max = 40) String phoneNumber) {
    }

    public record PayrollSettingsRequest(
            @Positive long monthlySalary,
            @Positive int monthlyRequiredHours) {
    }

    public record PayrollSettingsResponse(long monthlySalary, int monthlyRequiredHours) {
    }

    public record WorkerActiveRequest(@NotNull Boolean active) {
    }

    public record AttendanceRequest(
            @NotNull AttendanceStatus status,
            Instant checkIn,
            Instant checkOut) {
    }

    public enum AttendanceStatus {
        PRESENT,
        ABSENT
    }

    public record AttendanceResponse(
            Long workerId,
            LocalDate date,
            AttendanceStatus status,
            Instant checkIn,
            Instant checkOut,
            String recordedBy,
            Long fieldId,
            String fieldName,
            List<AttendanceSessionResponse> sessions) {
    }

    public record AttendanceSessionResponse(Instant checkIn, Instant checkOut) {
    }

    public record AttendanceFieldResponse(Long id, String name) {
    }

    public record AttendanceWorkerResponse(
            Long id, String name, String group, boolean active, Long fieldId, String fieldName) {
    }

    public record AttendanceTeamResponse(
            List<AttendanceFieldResponse> fields,
            List<AttendanceWorkerResponse> workers,
            List<AttendanceResponse> attendance) {
    }

    public enum TaskType {
        ASSIGNED_WORKERS,
        SEASONAL_WORKERS
    }

    public record TaskRequest(
            @NotBlank @Size(max = 240) String title,
            List<Long> assigneeIds,
            TaskType type,
            Long fieldId,
            @Size(max = 1000) String description,
            String priority,
            @Digits(integer = 8, fraction = 2) @Positive BigDecimal plannedHectares,
            @Positive Long pricePerHectare,
            List<Long> seasonalWorkerIds) {
    }

    public enum TaskStatus {
        IN_PROGRESS,
        TO_DO,
        DONE
    }

    public record TaskStatusRequest(
            @NotNull TaskStatus status,
            @PositiveOrZero Long actualCost,
            @PositiveOrZero Integer seasonalWorkerCount) {
    }

    public record TaskResponse(
            Long id,
            String title,
            List<Long> assigneeIds,
            String status,
            String type,
            Instant createdAt,
            Instant completedAt,
            Long actualCost,
            Integer seasonalWorkerCount,
            String createdBy,
            Long fieldId,
            String fieldName,
            List<EvidenceResponse> evidence,
            String description,
            String priority,
            BigDecimal plannedHectares,
            Long pricePerHectare,
            BigDecimal completedHectares,
            Long earnedAmount,
            Long remainingWorkCost,
            List<Long> seasonalWorkerIds,
            List<SeasonalWorkerResponse> seasonalWorkers,
            List<TaskProgressResponse> progressHistory,
            List<TaskAdvanceResponse> advances) {
    }

    public record SeasonalWorkerRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 40) String phoneNumber,
            Long fieldId) {
    }

    public record SeasonalWorkerAssignmentRequest(@NotNull List<@NotNull Long> seasonalWorkerIds) {
    }

    public record SeasonalWorkerResponse(
            Long id, String name, String phoneNumber, Long fieldId, String fieldName) {
    }

    public record TaskProgressRequest(
            @NotNull @Digits(integer = 8, fraction = 2) @PositiveOrZero BigDecimal completedHectares,
            @NotBlank @Size(max = 80) String idempotencyKey) {
    }

    public record TaskProgressResponse(
            Long id, BigDecimal completedHectares, Long earnedAmount, String reportedBy, Instant reportedAt) {
    }

    public record TaskAdvanceRequest(
            @Positive Long workerId,
            @Positive Long seasonalWorkerId,
            @Positive long amount,
            @NotBlank @Size(max = 80) String idempotencyKey) {
    }

    public record TaskAdvanceResponse(
            Long id,
            Long workerId,
            String workerName,
            Long seasonalWorkerId,
            String seasonalWorkerName,
            long amount,
            String paidBy,
            Instant paidAt) {
    }

    public record StockItemRequest(@NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 30) String unit, Long fieldId, @Positive Long unitPrice) {
    }

    public record StockItemUpdateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 30) String unit) {
    }

    public record StockItemPriceRequest(@NotNull @Positive Long unitPrice) {
    }

    public record StockItemResponse(
            Long id, String name, String unit, double quantity, Long unitPrice,
            Long fieldId, String fieldName) {
    }

    public record FieldMaterialNeedRequest(
            @NotNull Long fieldId,
            @NotBlank @Size(max = 120) String materialName,
            @NotBlank @Size(max = 30) String unit,
            @NotNull @Positive Double quantity,
            @Size(max = 240) String notes) {
    }

    public record FieldMaterialNeedPurchaseRequest(@NotNull Boolean purchased) {
    }

    public record FieldMaterialNeedResponse(
            Long id,
            Long fieldId,
            String fieldName,
            String materialName,
            String unit,
            double quantity,
            String notes,
            String createdBy,
            Instant createdAt,
            boolean purchased) {
    }

    public record StockItemPriceChangeResponse(
            Long id,
            Long itemId,
            Long previousUnitPrice,
            long newUnitPrice,
            Instant changedAt,
            String changedBy) {
    }

    public enum MovementKind {
        RECEIPT,
        USAGE
    }

    public record StockMovementRequest(
            @NotNull Long itemId,
            @NotNull MovementKind kind,
            @Positive double quantity,
            @NotBlank @Size(max = 240) String description,
            @Positive Long paidAmount) {
    }

    public record StockMovementResponse(
            Long id,
            Long itemId,
            String itemName,
            MovementKind kind,
            double quantity,
            String description,
            Instant recordedAt,
            String recordedBy,
            Long paidAmount,
            Long fieldId,
            String fieldName) {
    }

    public record StockOrderRequest(
            @NotNull Long itemId,
            @NotNull @Positive Double quantity,
            @NotBlank @Size(max = 240) String description) {
    }

    public record StockOrderDispatchRequest(
            @NotNull @Positive Double sentQuantity) {
    }

    public record StockOrderDecisionRequest(
            @NotNull Boolean approved,
            @Size(max = 240) String reason) {
    }

    public record StockOrderResponse(
            Long id,
            Long itemId,
            String itemName,
            String unit,
            double orderedQuantity,
            double receivedQuantity,
            String description,
            Instant orderedAt,
            String orderedBy,
            Instant sentAt,
            String sentBy,
            Instant receivedAt,
            String receivedBy,
            Long paidAmount,
            Long unitPriceAtDispatch,
            String status,
            Long fieldId,
            String fieldName,
            Instant technicalDecisionAt,
            String technicalDecisionBy,
            String technicalRejectionReason,
            String updatedBy,
            Instant updatedAt,
            Long revisedFromId,
            int revisionNumber) {
    }

    public record WorkPlanRequest(
            @NotBlank @Size(max = 240) String title,
            @NotNull Long fieldId,
            @NotNull Long fieldManagerAccountId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime) {
    }

    public record WorkPlanResponse(
            Long id,
            String title,
            Long fieldId,
            String fieldName,
            Long fieldManagerAccountId,
            String fieldManagerUsername,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String createdBy,
            Instant createdAt,
            String updatedBy,
            Instant updatedAt,
            String cancelledBy,
            Instant cancelledAt,
            String completedBy,
            Instant completedAt) {
    }

    public record WorkPlanAssigneeResponse(Long id, String username, Long fieldId, String fieldName) {
    }

    public record WorkPlanFieldResponse(Long id, String name, String location, String description) {
    }

    public enum MaterialAlertKind {
        MISSING,
        DELIVERY
    }

    public record MaterialAlertRequest(
            @NotNull Long itemId,
            @NotNull MaterialAlertKind kind,
            @Positive double quantity,
            @NotBlank @Size(max = 240) String description) {
    }

    public record MaterialAlertResponse(
            Long id,
            Long itemId,
            MaterialAlertKind kind,
            double quantity,
            String description,
            Instant recordedAt,
            String recordedBy,
            boolean resolved,
            Long fieldId,
            String fieldName) {
    }

    public record MaterialAlertResolutionRequest(@Positive Long paidAmount) {
    }

    public record ExpenseRequest(
            @NotBlank @Size(max = 240) String description,
            @Positive long amount,
            Long fieldId,
            String idempotencyKey,
            Instant occurredAt) {
    }

    public record FieldBudgetExpenseRequest(
            @NotBlank @Size(max = 240) String description,
            @Positive long amount,
            @NotBlank @Size(max = 36) String idempotencyKey,
            Instant occurredAt) {
    }

    public record OfflineExpenseSyncRequest(
            @NotBlank @Size(max = 240) String description,
            @Positive long amount,
            @NotNull Long fieldId,
            @NotBlank @Size(max = 36) String idempotencyKey,
            @NotNull Instant occurredAt) {
    }

    public record ExpenseResponse(
            Long id,
            String description,
            long amount,
            Instant recordedAt,
            Instant occurredAt,
            String recordedBy,
            Long fieldId,
            String fieldName,
            String idempotencyKey,
            boolean budgeted,
            String reconciliationStatus,
            List<EvidenceResponse> evidence) {
    }

    public record BudgetLimitRequest(@NotNull @PositiveOrZero Long fundingLimit) {
    }

    public record BudgetFundingRequest(
            @NotNull @Positive Long amount,
            @Size(max = 240) String note) {
    }

    public record BudgetAllocationRequest(
            @NotNull Long fieldId,
            @NotNull @Positive Long amount,
            @Size(max = 240) String note) {
    }

    public record FieldBudgetResponse(
            Long fieldId,
            String fieldName,
            long allocatedAmount,
            long remainingAmount,
            long pendingExpenses,
            boolean blocked) {
    }

    public record BudgetSnapshotResponse(
            long fundingLimit,
            long confirmedExpenses,
            long pendingExpenses,
            long remainingAmount,
            long unallocatedAmount,
            List<FieldBudgetResponse> fields) {
    }

    public record HarvestSaleRequest(
            @NotBlank @Size(max = 120) String product,
            @Positive double quantity,
            @Positive long totalPrice,
            Long fieldId) {
    }

    public record HarvestSaleResponse(
            Long id,
            String product,
            double quantity,
            long totalPrice,
            Instant recordedAt,
            String recordedBy,
            Long fieldId,
            String fieldName,
            List<EvidenceResponse> evidence) {
    }

    public record EvidenceResponse(
            Long id,
            String fileName,
            String contentType,
            String url,
            Instant uploadedAt) {
    }

    public record PaymentRequest(@NotNull Long workerId, @Positive long amount) {
    }

    public record PayrollPaymentRequestCreate(
            @NotNull Long workerId,
            @NotNull YearMonth through,
            @NotNull LocalDate scheduledPaymentDate) {
    }

    public record PayrollPaymentDecisionRequest(@NotNull Boolean approved) {
    }

    public record PayrollPaymentMarkPaidRequest(@NotNull LocalDate paidDate) {
    }

    public record PayrollDueResponse(
            Long workerId,
            long amount,
            long workedMinutes,
            LocalDate fromDate,
            LocalDate throughDate) {
    }

    public record PayrollPaymentRequestResponse(
            Long id,
            Long workerId,
            String workerName,
            long amount,
            long workedMinutes,
            LocalDate fromDate,
            LocalDate throughDate,
            LocalDate scheduledPaymentDate,
            String status,
            Instant requestedAt,
            String requestedBy,
            Instant decidedAt,
            String decidedBy,
            LocalDate paidAt,
            String paidBy) {
    }

    public record PaymentResponse(
            Long id,
            Long workerId,
            String workerName,
            long amount,
            Instant recordedAt,
            String recordedBy,
            Long fieldId,
            String fieldName,
            LocalDate paidThroughEnd) {
    }

    public record DailyReportResponse(
            List<AttendanceResponse> attendance,
            List<TaskResponse> tasks,
            List<StockMovementResponse> movements,
            List<MaterialAlertResponse> alerts,
            List<ExpenseResponse> expenses,
            List<PaymentResponse> payments,
            List<HarvestSaleResponse> sales,
            boolean validated,
            Instant validatedAt,
            String validatedBy) {
    }

    public record MonthlyReportResponse(
            List<LocalDate> validatedDates,
            List<AttendanceResponse> attendance,
            List<TaskResponse> tasks,
            List<StockMovementResponse> movements,
            List<MaterialAlertResponse> alerts,
            List<ExpenseResponse> expenses,
            List<PaymentResponse> payments,
            List<HarvestSaleResponse> sales,
            List<OvertimeResponse> overtime) {
    }

    public record DailyReportValidationResponse(
            LocalDate date,
            boolean validated,
            Instant validatedAt,
            String validatedBy) {
    }

    public record OvertimeRequest(
            @NotNull Long workerId,
            @Positive double hours,
            @NotBlank @Size(max = 240) String description) {
    }

    public record OvertimeResponse(
            Long id,
            Long workerId,
            String workerName,
            double hours,
            String description,
            Instant recordedAt,
            String recordedBy,
            Long fieldId,
            String fieldName) {
    }

    public record AccountCreateRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(min = 5, max = 12) String password,
            @NotNull UserRole role,
            @NotBlank @Size(max = 40) String phoneNumber,
            Long fieldId,
            @Positive Long monthlySalary,
            @Positive Integer monthlyRequiredHours) {
    }

    public record DirectorRegistrationRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(min = 5, max = 12) String password,
            @NotBlank @Size(max = 40) String phoneNumber) {
    }

    public record DirectorRegistrationStatus(boolean available) {
    }

    public record AccountEnabledRequest(@NotNull Boolean enabled) {
    }

    public record AccountUpdateRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 40) String phoneNumber,
            @Size(min = 5, max = 12) String password,
            Long fieldId,
            @Positive Long monthlySalary,
            @Positive Integer monthlyRequiredHours) {
    }

    public record AccountFieldRequest(@NotNull Long fieldId) {
    }

    public record AccountResponse(
            Long id, String username, UserRole role, boolean enabled,
            String phoneNumber, Long fieldId, String fieldName,
            Long payrollWorkerId, Long monthlySalary, Integer monthlyRequiredHours) {
    }

    public record FieldCreateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 240) String location,
            @Size(max = 1000) String description) {
    }

    public record FieldResponse(
            Long id,
            String name,
            String location,
            String description,
            boolean active,
            List<AccountResponse> managers) {
    }
}
