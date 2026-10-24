package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BudgetLedgerService {
    private final OperatingBudgetRepository budgets;
    private final FieldBudgetAllocationRepository allocations;
    private final BudgetLedgerEntryRepository ledger;
    private final ExpenseRepository expenses;

    public BudgetLedgerService(
            OperatingBudgetRepository budgets,
            FieldBudgetAllocationRepository allocations,
            BudgetLedgerEntryRepository ledger,
            ExpenseRepository expenses) {
        this.budgets = budgets;
        this.allocations = allocations;
        this.ledger = ledger;
        this.expenses = expenses;
    }

    public Snapshot snapshot() {
        OperatingBudget budget = requireBudget();
        long confirmed = expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.CONFIRMED);
        long pending = expenses.sumBudgetedAmountsByStatus(
                Expense.ReconciliationStatus.PENDING_RECONCILIATION);
        long remaining = budget.getFundingLimit() - confirmed - pending;
        List<FieldSnapshot> fieldSnapshots = allocations.findAllByOrderByIdAsc().stream()
                .map(allocation -> fieldSnapshot(allocation, pendingFor(allocation.getField().getId())))
                .toList();
        long allocatedRemaining = fieldSnapshots.stream()
                .mapToLong(FieldSnapshot::remainingAmount)
                .sum();
        return new Snapshot(
                budget.getFundingLimit(),
                confirmed,
                pending,
                remaining,
                Math.max(0, remaining - allocatedRemaining),
                fieldSnapshots);
    }

    public FieldSnapshot snapshotForField(PlantationField field) {
        FieldBudgetAllocation allocation = allocations.findByFieldId(field.getId())
                .orElse(null);
        if (allocation == null) {
            return new FieldSnapshot(field.getId(), field.getName(), 0, 0, 0, false);
        }
        return fieldSnapshot(allocation, pendingFor(field.getId()));
    }

    public void setFundingLimit(long newLimit, String actor) {
        if (newLimit < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The funding limit cannot be negative.");
        }
        OperatingBudget budget = requireBudgetForUpdate();
        long committed = expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.CONFIRMED)
                + expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.PENDING_RECONCILIATION)
                + allocations.findAllByOrderByIdAsc().stream()
                        .mapToLong(allocation -> fieldSnapshot(
                                allocation, pendingFor(allocation.getField().getId())).remainingAmount())
                        .sum();
        if (newLimit < committed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The funding limit cannot be lower than spent and allocated funds.");
        }
        long change = newLimit - budget.getFundingLimit();
        budget.updateLimit(newLimit, actor);
        ledger.save(new BudgetLedgerEntry(
                BudgetLedgerEntry.Kind.FUNDING_LIMIT_CHANGE,
                change,
                null,
                null,
                actor,
                Instant.now(),
                "Funding limit set to " + newLimit));
        reconcilePending();
    }

    public void addFunding(long amount, String actor, String note) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Funding addition must be greater than zero.");
        }
        OperatingBudget budget = requireBudgetForUpdate();
        final long newLimit;
        try {
            newLimit = Math.addExact(budget.getFundingLimit(), amount);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Funding limit is too large.");
        }
        budget.updateLimit(newLimit, actor);
        ledger.save(new BudgetLedgerEntry(
                BudgetLedgerEntry.Kind.FUNDING_ADDITION,
                amount,
                null,
                null,
                actor,
                Instant.now(),
                note));
        reconcilePending();
    }

    public void allocate(long fieldId, long amount, PlantationField field, String actor, String note) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocation must be greater than zero.");
        }
        OperatingBudget budget = requireBudgetForUpdate();
        FieldBudgetAllocation allocation = allocations.findByFieldIdForUpdate(fieldId).orElse(null);
        long availableToAllocate = availableToAllocate(budget);
        if (amount > availableToAllocate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient unallocated funds.");
        }
        if (allocation == null) {
            allocation = allocations.save(new FieldBudgetAllocation(field, amount));
        } else {
            allocation.allocate(amount);
        }
        ledger.save(new BudgetLedgerEntry(
                BudgetLedgerEntry.Kind.ALLOCATION, amount, field, null, actor, Instant.now(), note));
        reconcilePending();
    }

    public Expense recordBudgetedExpense(
            String description,
            long amount,
            String idempotencyKey,
            PlantationField field,
            String actor,
            Instant occurredAt) {
        OperatingBudget budget = requireBudgetForUpdate();
        Expense previous = expenses.findByRecordedByAndIdempotencyKey(actor, idempotencyKey).orElse(null);
        if (previous != null) return previous;
        FieldBudgetAllocation allocation = allocations.findByFieldIdForUpdate(field.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "No funds have been allocated to this field."));
        ensureAffordable(amount, budget, allocation);
        Expense expense = expenses.save(new Expense(
                description,
                amount,
                actor,
                field,
                occurredAt,
                idempotencyKey,
                true,
                Expense.ReconciliationStatus.CONFIRMED));
        allocation.spend(amount);
        ledger.save(new BudgetLedgerEntry(
                BudgetLedgerEntry.Kind.EXPENSE,
                amount,
                field,
                idempotencyKey,
                actor,
                occurredAt,
                description));
        return expense;
    }

    public Expense synchronizeOfflineExpense(
            String description,
            long amount,
            String idempotencyKey,
            PlantationField field,
            String actor,
            Instant occurredAt) {
        OperatingBudget budget = requireBudgetForUpdate();
        Expense previous = expenses.findByRecordedByAndIdempotencyKey(actor, idempotencyKey).orElse(null);
        if (previous != null) return previous;
        FieldBudgetAllocation allocation = allocations.findByFieldIdForUpdate(field.getId()).orElse(null);
        if (allocation == null) {
            allocation = allocations.save(new FieldBudgetAllocation(field, 0));
        }

        Expense expense = new Expense(
                description,
                amount,
                actor,
                field,
                occurredAt,
                idempotencyKey,
                true,
                Expense.ReconciliationStatus.PENDING_RECONCILIATION);
        expenses.save(expense);
        if (canCommitPending(amount, budget, allocation)) {
            confirmPending(expense, allocation);
        } else {
            allocation.setBlocked(true);
        }
        return expense;
    }

    private void reconcilePending() {
        OperatingBudget budget = requireBudgetForUpdate();
        List<Expense> pending = expenses.findByBudgetedTrueAndReconciliationStatusOrderByOccurredAtAsc(
                Expense.ReconciliationStatus.PENDING_RECONCILIATION);
        for (Expense expense : pending) {
            Long fieldId = expense.getField().getId();
            FieldBudgetAllocation allocation = allocations.findByFieldIdForUpdate(fieldId).orElse(null);
            if (allocation == null) continue;
            if (!canCommitPending(expense.getAmount(), budget, allocation)) continue;
            confirmPending(expense, allocation);
        }
        for (FieldBudgetAllocation allocation : allocations.findAllByOrderByIdAsc()) {
            if (expenses.findByBudgetedTrueAndReconciliationStatusAndFieldIdOrderByOccurredAtAsc(
                    Expense.ReconciliationStatus.PENDING_RECONCILIATION,
                    allocation.getField().getId()).isEmpty()) {
                allocation.setBlocked(false);
            }
        }
    }

    private void confirmPending(Expense expense, FieldBudgetAllocation allocation) {
        allocation.spend(expense.getAmount());
        expense.confirmBudgetedExpense();
        ledger.save(new BudgetLedgerEntry(
                BudgetLedgerEntry.Kind.EXPENSE,
                expense.getAmount(),
                expense.getField(),
                expense.getIdempotencyKey(),
                expense.getRecordedBy(),
                expense.getOccurredAt(),
                expense.getDescription()));
    }

    private boolean canCommitPending(
            long amount,
            OperatingBudget budget,
            FieldBudgetAllocation allocation) {
        if (amount <= 0 || allocation.getRemainingAmount() < amount) return false;
        long globalRemaining = budget.getFundingLimit()
                - expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.CONFIRMED)
                - expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.PENDING_RECONCILIATION);
        long pendingForField = pendingFor(allocation.getField().getId());
        return globalRemaining >= 0
                && pendingForField <= allocation.getRemainingAmount()
                && amount <= pendingForField;
    }

    private void ensureAffordable(long amount, OperatingBudget budget, FieldBudgetAllocation allocation) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense must be greater than zero.");
        }
        long pending = pendingFor(allocation.getField().getId());
        long fieldAvailable = allocation.getRemainingAmount() - pending;
        long globalAvailable = budget.getFundingLimit()
                - expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.CONFIRMED)
                - expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.PENDING_RECONCILIATION);
        if (allocation.isBlocked() || amount > fieldAvailable || amount > globalAvailable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient available funds for this field.");
        }
    }

    private long availableToAllocate(OperatingBudget budget) {
        long confirmed = expenses.sumBudgetedAmountsByStatus(Expense.ReconciliationStatus.CONFIRMED);
        long pending = expenses.sumBudgetedAmountsByStatus(
                Expense.ReconciliationStatus.PENDING_RECONCILIATION);
        long allocated = allocations.findAllByOrderByIdAsc().stream()
                .mapToLong(allocation -> fieldSnapshot(
                        allocation, pendingFor(allocation.getField().getId())).remainingAmount())
                .sum();
        return Math.max(0, budget.getFundingLimit() - confirmed - pending - allocated);
    }

    private long pendingFor(Long fieldId) {
        return expenses.sumBudgetedAmountsByFieldAndStatus(
                fieldId, Expense.ReconciliationStatus.PENDING_RECONCILIATION);
    }

    private FieldSnapshot fieldSnapshot(FieldBudgetAllocation allocation, long pending) {
        return new FieldSnapshot(
                allocation.getField().getId(),
                allocation.getField().getName(),
                allocation.getAllocatedAmount(),
                Math.max(0, allocation.getRemainingAmount() - pending),
                pending,
                allocation.isBlocked());
    }

    private OperatingBudget requireBudget() {
        return budgets.findById(OperatingBudget.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Operating budget has not been initialized."));
    }

    private OperatingBudget requireBudgetForUpdate() {
        return budgets.findByIdForUpdate(OperatingBudget.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Operating budget has not been initialized."));
    }

    public record Snapshot(
            long fundingLimit,
            long confirmedExpenses,
            long pendingExpenses,
            long remainingAmount,
            long unallocatedAmount,
            List<FieldSnapshot> fields) {
    }

    public record FieldSnapshot(
            Long fieldId,
            String fieldName,
            long allocatedAmount,
            long remainingAmount,
            long pendingExpenses,
            boolean blocked) {
    }
}
