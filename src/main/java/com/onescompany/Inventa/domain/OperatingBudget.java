package com.onescompany.Inventa.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "operating_budgets")
public class OperatingBudget {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private long fundingLimit;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, length = 80)
    private String updatedBy;

    @Version
    private long version;

    protected OperatingBudget() {
    }

    public OperatingBudget(long fundingLimit, String updatedBy) {
        this.id = SINGLETON_ID;
        updateLimit(fundingLimit, updatedBy);
    }

    public void updateLimit(long fundingLimit, String updatedBy) {
        this.fundingLimit = fundingLimit;
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;
    }

    public Long getId() { return id; }
    public long getFundingLimit() { return fundingLimit; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
