package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldTaskRepository extends JpaRepository<FieldTask, Long> {
    List<FieldTask> findByCreatedAtBetween(Instant start, Instant end);
}
