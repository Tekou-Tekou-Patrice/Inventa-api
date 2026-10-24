package com.onescompany.Inventa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payroll_settings")
public class PayrollSettings {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false)
    private long monthlySalary;

    @Column(nullable = false)
    private int monthlyRequiredHours;

    protected PayrollSettings() {
    }

    public PayrollSettings(long monthlySalary, int monthlyRequiredHours) {
        this.monthlySalary = monthlySalary;
        this.monthlyRequiredHours = monthlyRequiredHours;
    }

    public void update(long monthlySalary, int monthlyRequiredHours) {
        this.monthlySalary = monthlySalary;
        this.monthlyRequiredHours = monthlyRequiredHours;
    }

    public long getMonthlySalary() {
        return monthlySalary;
    }

    public int getMonthlyRequiredHours() {
        return monthlyRequiredHours;
    }
}
