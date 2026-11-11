package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldTaskProgressRepository extends JpaRepository<FieldTaskProgress, Long> {
    List<FieldTaskProgress> findByTaskIdOrderByReportedAtAscIdAsc(Long taskId);
    Optional<FieldTaskProgress> findFirstByTaskIdOrderByReportedAtDescIdDesc(Long taskId);
    Optional<FieldTaskProgress> findByReportedByAndIdempotencyKey(String reportedBy, String idempotencyKey);
}
