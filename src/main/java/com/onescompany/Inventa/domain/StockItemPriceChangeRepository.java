package com.onescompany.Inventa.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemPriceChangeRepository extends JpaRepository<StockItemPriceChange, Long> {
    List<StockItemPriceChange> findByItem_IdOrderByChangedAtDesc(Long itemId);
}
