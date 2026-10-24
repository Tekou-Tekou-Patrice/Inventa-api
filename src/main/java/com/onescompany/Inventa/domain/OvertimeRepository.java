package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OvertimeRepository extends JpaRepository<OvertimeRecord, Long> {
    List<OvertimeRecord> findTop100ByOrderByRecordedAtDesc();
    List<OvertimeRecord> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Instant start, Instant end);
}
