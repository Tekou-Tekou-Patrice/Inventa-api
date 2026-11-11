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
@Table(name = "field_material_needs")
public class FieldMaterialNeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private PlantationField field;

    @Column(nullable = false, length = 120)
    private String materialName;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false, length = 240)
    private String notes;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean purchased;

    protected FieldMaterialNeed() {
    }

    public FieldMaterialNeed(
            PlantationField field,
            String materialName,
            String unit,
            double quantity,
            String notes,
            String createdBy) {
        this.field = field;
        this.materialName = materialName;
        this.unit = unit;
        this.quantity = quantity;
        this.notes = notes == null ? "" : notes;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public PlantationField getField() { return field; }
    public String getMaterialName() { return materialName; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public String getNotes() { return notes; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
}
