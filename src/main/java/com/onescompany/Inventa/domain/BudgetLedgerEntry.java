package com.onescompany.Inventa.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "budget_ledger_entries")
public class BudgetLedgerEntry {
    public enum Kind {
        FUNDING_LIMIT_CHANGE,
        FUNDING_ADDITION,
        ALLOCATION,
        EXPENSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(nullable = false)
    private long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    @Column(length = 36)
    private String idempotencyKey;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(length = 240)
    private String note;

    protected BudgetLedgerEntry() {
    }

    public BudgetLedgerEntry(
            Kind kind,
            long amount,
            PlantationField field,
            String idempotencyKey,
            String actor,
            Instant occurredAt,
            String note) {
        this.kind = kind;
        this.amount = amount;
        this.field = field;
        this.idempotencyKey = idempotencyKey;
        this.actor = actor;
        this.occurredAt = occurredAt;
        this.recordedAt = Instant.now();
        this.note = note;
    }

    public Long getId() { return id; }
    public Kind getKind() { return kind; }
    public long getAmount() { return amount; }
    public PlantationField getField() { return field; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getNote() { return note; }
}
