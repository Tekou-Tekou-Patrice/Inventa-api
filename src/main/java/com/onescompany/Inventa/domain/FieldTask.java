package com.onescompany.Inventa.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "field_tasks")
public class FieldTask {
    public enum Status { IN_PROGRESS, TO_DO, DONE }
    public enum Type { ASSIGNED_WORKERS, SEASONAL_WORKERS }
    public enum Priority { URGENT, MEDIUM, NORMAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Priority priority = Priority.NORMAL;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "field_task_workers",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "worker_id"))
    private Set<Worker> assignees = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "field_task_seasonal_workers",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "seasonal_worker_id"))
    private Set<SeasonalWorker> seasonalAssignees = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Type type = Type.ASSIGNED_WORKERS;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    private Long actualCost;

    private Integer seasonalWorkerCount;

    @Column(precision = 10, scale = 2)
    private BigDecimal plannedHectares;

    private Long pricePerHectare;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    protected FieldTask() {
    }

    public FieldTask(String title, Set<Worker> assignees, String createdBy) {
        this(title, assignees, createdBy, null, Type.ASSIGNED_WORKERS);
    }

    public FieldTask(
            String title, Set<Worker> assignees, String createdBy, PlantationField field, Type type) {
        this(title, title, Priority.NORMAL, assignees, Set.of(), createdBy, field, type, null, null);
    }

    public FieldTask(
            String title,
            String description,
            Priority priority,
            Set<Worker> assignees,
            Set<SeasonalWorker> seasonalAssignees,
            String createdBy,
            PlantationField field,
            Type type,
            BigDecimal plannedHectares,
            Long pricePerHectare) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignees = assignees;
        this.seasonalAssignees = seasonalAssignees;
        this.status = Status.TO_DO;
        this.type = type;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
        this.field = field;
        this.plannedHectares = plannedHectares;
        this.pricePerHectare = pricePerHectare;
    }

    public void updateStatus(Status status, Long actualCost, Integer seasonalWorkerCount) {
        this.status = status;
        if (status == Status.DONE) {
            this.completedAt = Instant.now();
            this.actualCost = actualCost;
            this.seasonalWorkerCount = seasonalWorkerCount;
        } else {
            this.completedAt = null;
            this.actualCost = null;
            this.seasonalWorkerCount = null;
        }
    }
    public void assignField(PlantationField field) { this.field = field; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Set<Worker> getAssignees() { return assignees; }
    public Set<SeasonalWorker> getSeasonalAssignees() { return seasonalAssignees; }
    public Status getStatus() { return status; }
    public Type getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getActualCost() { return actualCost; }
    public Integer getSeasonalWorkerCount() { return seasonalWorkerCount; }
    public String getCreatedBy() { return createdBy; }
    public PlantationField getField() { return field; }
    public String getDescription() { return description == null ? title : description; }
    public Priority getPriority() { return priority == null ? Priority.NORMAL : priority; }
    public BigDecimal getPlannedHectares() { return plannedHectares; }
    public Long getPricePerHectare() { return pricePerHectare; }

    public void assignSeasonalWorkers(Set<SeasonalWorker> workers) {
        if (type != Type.SEASONAL_WORKERS) {
            throw new IllegalStateException("Only a seasonal task can have seasonal workers.");
        }
        this.seasonalAssignees = new HashSet<>(workers);
    }
}
