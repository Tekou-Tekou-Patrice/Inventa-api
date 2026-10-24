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
@Table(name = "stock_item_price_changes")
public class StockItemPriceChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem item;

    private Long previousUnitPrice;

    @Column(nullable = false)
    private long newUnitPrice;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(nullable = false, length = 80)
    private String changedBy;

    protected StockItemPriceChange() {
    }

    public StockItemPriceChange(StockItem item, Long previousUnitPrice, long newUnitPrice, String changedBy) {
        this.item = item;
        this.previousUnitPrice = previousUnitPrice;
        this.newUnitPrice = newUnitPrice;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public StockItem getItem() { return item; }
    public Long getPreviousUnitPrice() { return previousUnitPrice; }
    public long getNewUnitPrice() { return newUnitPrice; }
    public Instant getChangedAt() { return changedAt; }
    public String getChangedBy() { return changedBy; }
}
