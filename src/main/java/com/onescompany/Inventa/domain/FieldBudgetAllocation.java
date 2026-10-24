package com.onescompany.Inventa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "field_budget_allocations", uniqueConstraints = @UniqueConstraint(
        name = "uk_field_budget_allocation_field", columnNames = "field_id"))
public class FieldBudgetAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private PlantationField field;

    @Column(nullable = false)
    private long allocatedAmount;

    @Column(nullable = false)
    private long remainingAmount;

    @Column(nullable = false)
    private boolean blocked;

    protected FieldBudgetAllocation() {
    }

    public FieldBudgetAllocation(PlantationField field, long amount) {
        this.field = field;
        this.allocatedAmount = amount;
        this.remainingAmount = amount;
    }

    public void allocate(long amount) {
        allocatedAmount = Math.addExact(allocatedAmount, amount);
        remainingAmount = Math.addExact(remainingAmount, amount);
    }

    public void spend(long amount) {
        if (amount <= 0 || amount > remainingAmount) {
            throw new IllegalArgumentException("The field allocation does not have enough funds.");
        }
        remainingAmount -= amount;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Long getId() { return id; }
    public PlantationField getField() { return field; }
    public long getAllocatedAmount() { return allocatedAmount; }
    public long getRemainingAmount() { return remainingAmount; }
    public boolean isBlocked() { return blocked; }
}
