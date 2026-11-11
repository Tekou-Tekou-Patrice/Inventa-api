package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FieldTaskRepository extends JpaRepository<FieldTask, Long> {
    List<FieldTask> findByCreatedAtBetween(Instant start, Instant end);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from FieldTask t where t.id = :id")
    java.util.Optional<FieldTask> findByIdForUpdate(@Param("id") Long id);
}
