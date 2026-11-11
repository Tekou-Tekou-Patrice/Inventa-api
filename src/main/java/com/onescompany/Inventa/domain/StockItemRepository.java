package com.onescompany.Inventa.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndFieldId(String name, Long fieldId);

    boolean existsByNameIgnoreCaseAndFieldIdAndIdNot(String name, Long fieldId, Long id);
}
