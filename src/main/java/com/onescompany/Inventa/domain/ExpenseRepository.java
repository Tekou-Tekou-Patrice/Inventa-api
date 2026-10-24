package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findTop100ByOrderByRecordedAtDesc();
    List<Expense> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Instant start, Instant end);
    Optional<Expense> findByRecordedByAndIdempotencyKey(String recordedBy, String idempotencyKey);
    List<Expense> findByBudgetedTrueAndReconciliationStatusAndFieldIdOrderByOccurredAtAsc(
            Expense.ReconciliationStatus status, Long fieldId);
    List<Expense> findByBudgetedTrueAndReconciliationStatusOrderByOccurredAtAsc(
            Expense.ReconciliationStatus status);

    @Query("select coalesce(sum(expense.amount), 0) from Expense expense "
            + "where expense.budgeted = true and expense.reconciliationStatus = :status")
    long sumBudgetedAmountsByStatus(@Param("status") Expense.ReconciliationStatus status);

    @Query("select coalesce(sum(expense.amount), 0) from Expense expense "
            + "where expense.budgeted = true and expense.reconciliationStatus = :status "
            + "and expense.field.id = :fieldId")
    long sumBudgetedAmountsByFieldAndStatus(
            @Param("fieldId") Long fieldId,
            @Param("status") Expense.ReconciliationStatus status);
}
