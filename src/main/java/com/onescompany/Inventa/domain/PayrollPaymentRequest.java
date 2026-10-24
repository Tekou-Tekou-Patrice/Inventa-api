package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "payroll_payment_requests")
public class PayrollPaymentRequest {
    public enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        PAID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long workedMinutes;

    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate throughDate;

    @Column(nullable = false)
    private LocalDate scheduledPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status;

    @Column(nullable = false)
    private Instant requestedAt;

    @Column(nullable = false, length = 80)
    private String requestedBy;

    private Instant decidedAt;
    private String decidedBy;
    private LocalDate paidAt;
    private String paidBy;

    protected PayrollPaymentRequest() {
    }

    public PayrollPaymentRequest(
            Worker worker,
            PayrollDueData due,
            LocalDate scheduledPaymentDate,
            String requestedBy) {
        this.worker = worker;
        this.amount = due.amount();
        this.workedMinutes = due.workedMinutes();
        this.fromDate = due.fromDate();
        this.throughDate = due.throughDate();
        this.scheduledPaymentDate = scheduledPaymentDate;
        this.status = Status.PENDING;
        this.requestedAt = Instant.now();
        this.requestedBy = requestedBy;
    }

    public void decide(boolean approved, String username) {
        this.status = approved ? Status.APPROVED : Status.REJECTED;
        this.decidedAt = Instant.now();
        this.decidedBy = username;
    }

    public void markPaid(LocalDate paidAt, String username) {
        this.status = Status.PAID;
        this.paidAt = paidAt;
        this.paidBy = username;
    }

    public Long getId() { return id; }
    public Worker getWorker() { return worker; }
    public long getAmount() { return amount; }
    public long getWorkedMinutes() { return workedMinutes; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getThroughDate() { return throughDate; }
    public LocalDate getScheduledPaymentDate() { return scheduledPaymentDate; }
    public Status getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getDecidedBy() { return decidedBy; }
    public LocalDate getPaidAt() { return paidAt; }
    public String getPaidBy() { return paidBy; }

    public record PayrollDueData(long amount, long workedMinutes, LocalDate fromDate, LocalDate throughDate) {
    }
}
