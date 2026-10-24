package com.onescompany.Inventa.domain;

import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop100ByOrderByRecordedAtDesc();
    List<StockMovement> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Instant start, Instant end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from StockItem item where item.id = :id")
    java.util.Optional<StockItem> findItemForUpdate(@Param("id") Long id);
}
