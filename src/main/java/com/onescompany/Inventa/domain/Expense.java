package com.onescompany.Inventa.domain;

import java.time.Instant;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "expenses", uniqueConstraints = @UniqueConstraint(
        name = "uk_expense_account_idempotency", columnNames = { "recorded_by", "idempotency_key" }))
public class Expense {
    public enum ReconciliationStatus {
        CONFIRMED,
        PENDING_RECONCILIATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 80)
    private String recordedBy;

    @Column
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean budgeted;

    @Column(length = 36)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ReconciliationStatus reconciliationStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    protected Expense() {
    }

    public Expense(String description, long amount, String recordedBy) {
        this(description, amount, recordedBy, null);
    }

    public Expense(String description, long amount, String recordedBy, PlantationField field) {
        this(description, amount, recordedBy, field, Instant.now(), null, false,
                ReconciliationStatus.CONFIRMED);
    }

    public Expense(
            String description,
            long amount,
            String recordedBy,
            PlantationField field,
            Instant occurredAt,
            String idempotencyKey,
            boolean budgeted,
            ReconciliationStatus reconciliationStatus) {
        this.description = description;
        this.amount = amount;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
        this.budgeted = budgeted;
        this.reconciliationStatus = reconciliationStatus;
        this.field = field;
    }

    public void confirmBudgetedExpense() {
        this.reconciliationStatus = ReconciliationStatus.CONFIRMED;
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public long getAmount() { return amount; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getOccurredAt() { return occurredAt == null ? recordedAt : occurredAt; }
    public String getRecordedBy() { return recordedBy; }
    public boolean isBudgeted() { return budgeted; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus == null ? ReconciliationStatus.CONFIRMED : reconciliationStatus;
    }
    public PlantationField getField() { return field; }
    public void assignField(PlantationField field) { this.field = field; }
}
