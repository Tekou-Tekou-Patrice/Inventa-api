package com.onescompany.Inventa.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceAttachmentRepository extends JpaRepository<EvidenceAttachment, Long> {
    List<EvidenceAttachment> findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(
            EvidenceAttachment.EntityType entityType,
            Long entityId);

    long countByEntityTypeAndEntityId(EvidenceAttachment.EntityType entityType, Long entityId);
}
