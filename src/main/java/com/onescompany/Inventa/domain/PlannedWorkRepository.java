package com.onescompany.Inventa.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannedWorkRepository extends JpaRepository<PlannedWork, Long> {
    List<PlannedWork> findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(
            LocalDate to, LocalDate from);

    List<PlannedWork> findByFieldManagerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(
            Long fieldManagerId, LocalDate to, LocalDate from);
}
