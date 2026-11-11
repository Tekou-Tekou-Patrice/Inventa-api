package com.onescompany.Inventa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "workers")
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String groupName;

    @Column(nullable = false)
    private long salary;

    @Column(columnDefinition = "integer default 208")
    private Integer monthlyRequiredHours;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean salaryIsIndividual;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 40)
    private String phoneNumber = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    protected Worker() {
    }

    public Worker(String name, String groupName, long salary) {
        this.name = name;
        this.groupName = groupName;
        this.salary = salary;
        this.active = true;
    }

    public Worker(String name, String groupName, long salary, PlantationField field) {
        this(name, groupName, salary);
        this.field = field;
    }

    public Worker(String name, String groupName, long salary, String phoneNumber, PlantationField field) {
        this(name, groupName, salary, field);
        this.phoneNumber = phoneNumber;
    }

    public Worker(
            String name,
            String groupName,
            long salary,
            int monthlyRequiredHours,
            String phoneNumber,
            PlantationField field) {
        this(name, groupName, salary, phoneNumber, field);
        this.monthlyRequiredHours = monthlyRequiredHours;
    }

    public void updateSalary(long salary) {
        this.salary = salary;
        this.salaryIsIndividual = true;
    }
    public void updateName(String name) { this.name = name; }
    public void updateMonthlyRequiredHours(int hours) { this.monthlyRequiredHours = hours; }
    public void updateDefaultSalary(long salary) {
        if (!salaryIsIndividual) {
            this.salary = salary;
        }
    }
    public void updateActive(boolean active) { this.active = active; }
    public void updatePhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void assignField(PlantationField field) { this.field = field; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getGroupName() { return groupName; }
    public long getSalary() { return salary; }
    public Integer getMonthlyRequiredHours() { return monthlyRequiredHours; }
    public boolean isSalaryIsIndividual() { return salaryIsIndividual; }
    public boolean isActive() { return active; }
    public String getPhoneNumber() { return phoneNumber; }
    public PlantationField getField() { return field; }
}
