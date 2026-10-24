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
@Table(name = "harvest_sales")
public class HarvestSale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String product;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false)
    private long totalPrice;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 80)
    private String recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    protected HarvestSale() {
    }

    public HarvestSale(
            String product,
            double quantity,
            long totalPrice,
            String recordedBy,
            PlantationField field) {
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
        this.field = field;
    }

    public Long getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public double getQuantity() {
        return quantity;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public PlantationField getField() {
        return field;
    }
}
