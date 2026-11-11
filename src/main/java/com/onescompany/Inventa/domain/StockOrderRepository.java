package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {
    List<StockOrder> findTop100ByOrderByOrderedAtDesc();

    boolean existsByItem_IdAndStatusIn(Long itemId, Collection<StockOrder.Status> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stockOrder from StockOrder stockOrder join fetch stockOrder.item where stockOrder.id = :id")
    Optional<StockOrder> findByIdForUpdate(@Param("id") Long id);
}
