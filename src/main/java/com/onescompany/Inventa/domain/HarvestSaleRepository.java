package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HarvestSaleRepository extends JpaRepository<HarvestSale, Long> {
    List<HarvestSale> findTop100ByOrderByRecordedAtDesc();

    List<HarvestSale> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Instant start,
            Instant end);
}
