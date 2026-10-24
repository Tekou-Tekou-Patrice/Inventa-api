package com.onescompany.Inventa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "stock_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_item_name_field", columnNames = { "name", "field_id" }))
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false)
    private double quantity;

    private Long unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private PlantationField field;

    protected StockItem() {
    }

    public StockItem(String name, String unit) {
        this(name, unit, null);
    }

    public StockItem(String name, String unit, PlantationField field) {
        this.name = name;
        this.unit = unit;
        this.quantity = 0;
        this.field = field;
    }

    public void adjustQuantity(double delta) {
        this.quantity += delta;
    }
    public void updateUnitPrice(long unitPrice) {
        if (unitPrice <= 0) {
            throw new IllegalArgumentException("The unit price must be greater than zero.");
        }
        this.unitPrice = unitPrice;
    }
    public void assignField(PlantationField field) { this.field = field; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public Long getUnitPrice() { return unitPrice; }
    public PlantationField getField() { return field; }
}
