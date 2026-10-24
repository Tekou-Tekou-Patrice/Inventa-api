package com.onescompany.Inventa.domain;

import java.time.Instant;

import com.onescompany.Inventa.security.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    public enum Kind { RECEIPT, USAGE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private StockItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Kind kind;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 80)
    private String recordedBy;

    private Long paidAmount;

    protected StockMovement() {
    }

    public StockMovement(StockItem item, Kind kind, double quantity, String description,
            String recordedBy, Long paidAmount) {
        this.item = item;
        this.kind = kind;
        this.quantity = quantity;
        this.description = description;
        this.recordedAt = Instant.now();
        this.recordedBy = recordedBy;
        this.paidAmount = paidAmount;
    }

    public Long getId() { return id; }
    public StockItem getItem() { return item; }
    public Kind getKind() { return kind; }
    public double getQuantity() { return quantity; }
    public String getDescription() { return description; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public Long getPaidAmount() { return paidAmount; }
}
