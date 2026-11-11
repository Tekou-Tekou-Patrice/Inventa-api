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
        PENDING_TECHNICAL_APPROVAL,
        TECHNICALLY_APPROVED,
        TECHNICALLY_REJECTED,
        CANCELLED,
        SENT,
        RECEIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private StockItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_field_id")
    private PlantationField field;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revised_from_id")
    private StockOrder revisedFrom;

    @Column(nullable = false, columnDefinition = "integer default 1")
    private int revisionNumber = 1;

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
    @Column(nullable = false, length = 32)
    private Status status;
    private Instant technicalDecisionAt;
    @Column(length = 80)
    private String technicalDecisionBy;
    @Column(length = 240)
    private String technicalRejectionReason;
    private String updatedBy;
    private Instant updatedAt;

    protected StockOrder() {
    }

    public StockOrder(StockItem item, double orderedQuantity, String description, String orderedBy) {
        this.item = item;
        this.field = item.getField();
        this.orderedQuantity = orderedQuantity;
        this.description = description;
        this.orderedBy = orderedBy;
        this.orderedAt = Instant.now();
        this.status = Status.PENDING_TECHNICAL_APPROVAL;
    }

    public void decide(boolean approved, String reason, String decidedBy) {
        if (status != Status.PENDING_TECHNICAL_APPROVAL && status != Status.ORDERED) {
            throw new IllegalStateException("Only a pending stock order can receive a technical decision.");
        }
        if (!approved && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }
        this.technicalDecisionAt = Instant.now();
        this.technicalDecisionBy = decidedBy;
        this.technicalRejectionReason = approved ? null : reason.trim();
        this.status = approved ? Status.TECHNICALLY_APPROVED : Status.TECHNICALLY_REJECTED;
    }

    public StockOrder(StockItem item, double quantity, String description, String orderedBy, StockOrder revisedFrom) {
        this(item, quantity, description, orderedBy);
        this.field = revisedFrom.getField();
        this.revisedFrom = revisedFrom;
        this.revisionNumber = revisedFrom.getRevisionNumber() + 1;
    }

    public void updateRequest(StockItem item, double quantity, String description, String updatedBy) {
        if (status != Status.PENDING_TECHNICAL_APPROVAL && status != Status.ORDERED) {
            throw new IllegalStateException("Only a pending stock request can be edited.");
        }
        if (!item.getField().getId().equals(getField().getId())) {
            throw new IllegalArgumentException("A request cannot be moved to another field.");
        }
        this.item = item;
        this.orderedQuantity = quantity;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void cancel(String cancelledBy) {
        if (status != Status.PENDING_TECHNICAL_APPROVAL && status != Status.ORDERED) {
            throw new IllegalStateException("Only a pending stock request can be cancelled.");
        }
        this.status = Status.CANCELLED;
        this.updatedBy = cancelledBy;
        this.updatedAt = Instant.now();
    }

    public void dispatch(double sentQuantity, long unitPrice, String sentBy) {
        if (status != Status.TECHNICALLY_APPROVED) {
            throw new IllegalStateException("Technical approval is required before dispatch.");
        }
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
    public PlantationField getField() { return field != null ? field : item.getField(); }
    public StockOrder getRevisedFrom() { return revisedFrom; }
    public Long getRevisedFromId() { return revisedFrom == null ? null : revisedFrom.getId(); }
    public int getRevisionNumber() { return revisionNumber; }
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
    public Instant getTechnicalDecisionAt() { return technicalDecisionAt; }
    public String getTechnicalDecisionBy() { return technicalDecisionBy; }
    public String getTechnicalRejectionReason() { return technicalRejectionReason; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
