package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
@Table(name = "stock_orders")
public class StockOrder {
    public enum Status {
        ORDERED,
        SENT,
        RECEIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private StockItem item;

    @Column(nullable = false)
    private double orderedQuantity;

    @Column(nullable = false)
    private double receivedQuantity;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false)
    private Instant orderedAt;

    @Column(nullable = false, length = 80)
    private String orderedBy;

    private Instant receivedAt;
    private String receivedBy;
    private Long paidAmount;
    private Long unitPriceAtDispatch;
    private Instant sentAt;
    private String sentBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status;

    protected StockOrder() {
    }

    public StockOrder(StockItem item, double orderedQuantity, String description, String orderedBy) {
        this.item = item;
        this.orderedQuantity = orderedQuantity;
        this.description = description;
        this.orderedBy = orderedBy;
        this.orderedAt = Instant.now();
        this.status = Status.ORDERED;
    }

    public void dispatch(double sentQuantity, long unitPrice, String sentBy) {
        this.receivedQuantity = sentQuantity;
        this.unitPriceAtDispatch = unitPrice;
        this.paidAmount = calculateTotal(unitPrice, sentQuantity);
        this.sentBy = sentBy;
        this.sentAt = Instant.now();
        this.status = Status.SENT;
    }

    public static long calculateTotal(long unitPrice, double quantity) {
        try {
            return BigDecimal.valueOf(unitPrice)
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("The shipment total is too large.", exception);
        }
    }

    public void receive(String receivedBy) {
        this.receivedBy = receivedBy;
        this.receivedAt = Instant.now();
        this.status = Status.RECEIVED;
    }

    public Long getId() { return id; }
    public StockItem getItem() { return item; }
    public double getOrderedQuantity() { return orderedQuantity; }
    public double getReceivedQuantity() { return receivedQuantity; }
    public String getDescription() { return description; }
    public Instant getOrderedAt() { return orderedAt; }
    public String getOrderedBy() { return orderedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getReceivedBy() { return receivedBy; }
    public Long getPaidAmount() { return paidAmount; }
    public Long getUnitPriceAtDispatch() { return unitPriceAtDispatch; }
    public Instant getSentAt() { return sentAt; }
    public String getSentBy() { return sentBy; }
    public Status getStatus() { return status; }
}
