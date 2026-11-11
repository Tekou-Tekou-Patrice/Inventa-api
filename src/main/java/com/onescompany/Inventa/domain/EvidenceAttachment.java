package com.onescompany.Inventa.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidence_attachments")
public class EvidenceAttachment {
    public enum EntityType { SALE, TASK, EXPENSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12)
    private EntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column
    private Long fieldId;

    @Column(nullable = false, length = 240)
    private String fileName;

    @Column(nullable = false, length = 80)
    private String contentType;

    @Column(nullable = false, length = 80)
    private String storageKey;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 80)
    private String createdBy;

    protected EvidenceAttachment() {
    }

    public EvidenceAttachment(
            EntityType entityType,
            Long entityId,
            Long fieldId,
            String fileName,
            String contentType,
            String storageKey,
            String createdBy) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.fieldId = fieldId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.storageKey = storageKey;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public EntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public Long getFieldId() { return fieldId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public String getStorageKey() { return storageKey; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
