package com.onescompany.Inventa.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "field_task_advances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_task_advance_account_key",
                columnNames = {"paid_by", "idempotency_key"}))
public class FieldTaskAdvance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private FieldTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seasonal_worker_id")
    private SeasonalWorker seasonalWorker;

    @Column(nullable = false)
    private long amount;

    @Column(name = "paid_by", nullable = false, length = 80)
    private String paidBy;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant paidAt;

    protected FieldTaskAdvance() {
    }

    public FieldTaskAdvance(
            FieldTask task,
            Worker worker,
            SeasonalWorker seasonalWorker,
            long amount,
            String paidBy,
            String idempotencyKey) {
        this.task = task;
        this.worker = worker;
        this.seasonalWorker = seasonalWorker;
        this.amount = amount;
        this.paidBy = paidBy;
        this.idempotencyKey = idempotencyKey;
        this.paidAt = Instant.now();
    }

    public Long getId() { return id; }
    public FieldTask getTask() { return task; }
    public Worker getWorker() { return worker; }
    public SeasonalWorker getSeasonalWorker() { return seasonalWorker; }
    public long getAmount() { return amount; }
    public String getPaidBy() { return paidBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getPaidAt() { return paidAt; }
}
