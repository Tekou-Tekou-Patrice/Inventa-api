package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "validated_field_tasks")
public class ValidatedFieldTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false, length = 240)
    private String title;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "validated_field_task_workers", joinColumns = @JoinColumn(name = "snapshot_id"))
    @Column(name = "worker_id", nullable = false)
    private List<Long> assigneeIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "validated_field_task_evidence", joinColumns = @JoinColumn(name = "snapshot_id"))
    @Column(name = "evidence_id", nullable = false)
    private List<Long> evidenceIds = new ArrayList<>();

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 24)
    private String type;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;
    private Long actualCost;
    private Integer seasonalWorkerCount;

    @Column(nullable = false, length = 80)
    private String createdBy;

    private Long fieldId;

    @Column(length = 120)
    private String fieldName;

    @Column(nullable = false, length = 80)
    private String validatedBy;

    @Column(nullable = false)
    private Instant validatedAt;

    protected ValidatedFieldTask() {
    }

    public ValidatedFieldTask(
            FieldTask task,
            LocalDate reportDate,
            String validatedBy,
            Instant validatedAt,
            List<Long> evidenceIds) {
        this.taskId = task.getId();
        this.reportDate = reportDate;
        this.title = task.getTitle();
        this.assigneeIds = task.getAssignees().stream().map(Worker::getId).toList();
        this.evidenceIds = new ArrayList<>(evidenceIds);
        this.status = task.getStatus().name();
        this.type = task.getType() == null
                ? FieldTask.Type.ASSIGNED_WORKERS.name()
                : task.getType().name();
        this.createdAt = task.getCreatedAt();
        this.completedAt = task.getCompletedAt();
        this.actualCost = task.getActualCost();
        this.seasonalWorkerCount = task.getSeasonalWorkerCount();
        this.createdBy = task.getCreatedBy();
        this.fieldId = task.getField() == null ? null : task.getField().getId();
        this.fieldName = task.getField() == null ? null : task.getField().getName();
        this.validatedBy = validatedBy;
        this.validatedAt = validatedAt;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public LocalDate getReportDate() { return reportDate; }
    public String getTitle() { return title; }
    public List<Long> getAssigneeIds() { return assigneeIds; }
    public List<Long> getEvidenceIds() { return evidenceIds; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getActualCost() { return actualCost; }
    public Integer getSeasonalWorkerCount() { return seasonalWorkerCount; }
    public String getCreatedBy() { return createdBy; }
    public Long getFieldId() { return fieldId; }
    public String getFieldName() { return fieldName; }
    public String getValidatedBy() { return validatedBy; }
    public Instant getValidatedAt() { return validatedAt; }
}
