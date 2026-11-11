package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.onescompany.Inventa.security.AppUser;

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
@Table(name = "planned_work")
public class PlannedWork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private PlantationField field;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_manager_account_id", nullable = false)
    private AppUser fieldManager;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 80)
    private String updatedBy;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant cancelledAt;

    @Column(length = 80)
    private String cancelledBy;

    private Instant completedAt;

    @Column(length = 80)
    private String completedBy;

    protected PlannedWork() {
    }

    public PlannedWork(
            String title,
            PlantationField field,
            AppUser fieldManager,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String createdBy) {
        this.title = title;
        this.field = field;
        this.fieldManager = fieldManager;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedBy = createdBy;
        this.updatedAt = this.createdAt;
    }

    public void update(
            String title,
            PlantationField field,
            AppUser fieldManager,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String updatedBy) {
        if (cancelledAt != null || completedAt != null) {
            throw new IllegalStateException("A cancelled or completed plan cannot be edited.");
        }
        this.title = title;
        this.field = field;
        this.fieldManager = fieldManager;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void cancel(String cancelledBy) {
        if (cancelledAt != null || completedAt != null) {
            throw new IllegalStateException("A completed or cancelled plan cannot be cancelled.");
        }
        this.cancelledBy = cancelledBy;
        this.cancelledAt = Instant.now();
        this.updatedBy = cancelledBy;
        this.updatedAt = cancelledAt;
    }

    public void complete(String completedBy) {
        if (cancelledAt != null || completedAt != null) {
            throw new IllegalStateException("This plan cannot be completed.");
        }
        this.completedBy = completedBy;
        this.completedAt = Instant.now();
        this.updatedBy = completedBy;
        this.updatedAt = completedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public PlantationField getField() { return field; }
    public AppUser getFieldManager() { return fieldManager; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public boolean isCancelled() { return cancelledAt != null; }
    public Instant getCompletedAt() { return completedAt; }
    public String getCompletedBy() { return completedBy; }
    public boolean isCompleted() { return completedAt != null; }
}
