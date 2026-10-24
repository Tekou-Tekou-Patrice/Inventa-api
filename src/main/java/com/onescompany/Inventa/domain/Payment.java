package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 80)
    private String recordedBy;

    private LocalDate paidThroughEnd;

    protected Payment() {
    }

    public Payment(Worker worker, long amount, String recordedBy) {
        this.worker = worker;
        this.amount = amount;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }

    public Payment(Worker worker, long amount, String recordedBy, LocalDate paidThroughEnd) {
        this(worker, amount, recordedBy);
        this.paidThroughEnd = paidThroughEnd;
    }

    public Payment(Worker worker, long amount, String recordedBy, LocalDate paidThroughEnd, LocalDate paidDate) {
        this(worker, amount, recordedBy, paidThroughEnd);
        this.recordedAt = paidDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    }

    public Long getId() { return id; }
    public Worker getWorker() { return worker; }
    public long getAmount() { return amount; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public LocalDate getPaidThroughEnd() { return paidThroughEnd; }
}
