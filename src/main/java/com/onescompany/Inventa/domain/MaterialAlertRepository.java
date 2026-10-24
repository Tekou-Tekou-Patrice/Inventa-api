package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface MaterialAlertRepository extends JpaRepository<MaterialAlert, Long> {
    List<MaterialAlert> findTop100ByOrderByRecordedAtDesc();
    List<MaterialAlert> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Instant start, Instant end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select alert from MaterialAlert alert join fetch alert.item where alert.id = :id")
    Optional<MaterialAlert> findByIdForUpdate(@Param("id") Long id);
}
