package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "daily_report_validations")
public class DailyReportValidation {
    @Id
    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(nullable = false, length = 80)
    private String validatedBy;

    @Column(nullable = false)
    private Instant validatedAt;

    protected DailyReportValidation() {
    }

    public DailyReportValidation(LocalDate reportDate, String validatedBy) {
        this.reportDate = reportDate;
        this.validatedBy = validatedBy;
        this.validatedAt = Instant.now();
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }
}
