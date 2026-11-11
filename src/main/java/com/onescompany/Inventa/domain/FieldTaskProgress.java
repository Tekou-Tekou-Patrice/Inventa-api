package com.onescompany.Inventa.domain;

import java.math.BigDecimal;
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
        name = "field_task_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_task_progress_account_key",
                columnNames = {"reported_by", "idempotency_key"}))
public class FieldTaskProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private FieldTask task;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal completedHectares;

    @Column(nullable = false)
    private long earnedAmount;

    @Column(nullable = false, length = 80)
    private String reportedBy;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant reportedAt;

    protected FieldTaskProgress() {
    }

    public FieldTaskProgress(
            FieldTask task,
            BigDecimal completedHectares,
            long earnedAmount,
            String reportedBy,
            String idempotencyKey) {
        this.task = task;
        this.completedHectares = completedHectares;
        this.earnedAmount = earnedAmount;
        this.reportedBy = reportedBy;
        this.idempotencyKey = idempotencyKey;
        this.reportedAt = Instant.now();
    }

    public Long getId() { return id; }
    public FieldTask getTask() { return task; }
    public BigDecimal getCompletedHectares() { return completedHectares; }
    public long getEarnedAmount() { return earnedAmount; }
    public String getReportedBy() { return reportedBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getReportedAt() { return reportedAt; }
}
