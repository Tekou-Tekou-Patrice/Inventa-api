package com.onescompany.Inventa.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonalWorkerRepository extends JpaRepository<SeasonalWorker, Long> {
    List<SeasonalWorker> findByFieldIdAndActiveTrueOrderByNameAsc(Long fieldId);
}
