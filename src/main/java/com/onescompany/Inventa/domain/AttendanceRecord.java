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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "attendance_records", uniqueConstraints = @UniqueConstraint(columnNames = { "worker_id", "work_date" }))
public class AttendanceRecord {
    public enum Status {
        PRESENT,
        ABSENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status;

    private Instant checkIn;

    private Instant checkOut;

    @Column(nullable = false, length = 80)
    private String recordedBy;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(
            Worker worker, LocalDate date, Status status, Instant checkIn, Instant checkOut, String recordedBy) {
        this.worker = worker;
        this.date = date;
        this.status = status;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.recordedBy = recordedBy;
    }

    public void update(Status status, Instant checkIn, Instant checkOut, String recordedBy) {
        this.status = status;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.recordedBy = recordedBy;
    }

    public Long getId() { return id; }
    public Worker getWorker() { return worker; }
    public LocalDate getDate() { return date; }
    public Status getStatus() { return status; }
    public Instant getCheckIn() { return checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public String getRecordedBy() { return recordedBy; }
}
