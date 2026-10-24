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

@Entity
@Table(name = "overtime_records")
public class OvertimeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false)
    private double hours;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 80)
    private String recordedBy;

    protected OvertimeRecord() {
    }

    public OvertimeRecord(Worker worker, double hours, String description, String recordedBy) {
        this.worker = worker;
        this.hours = hours;
        this.description = description;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Worker getWorker() { return worker; }
    public double getHours() { return hours; }
    public String getDescription() { return description; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
}
