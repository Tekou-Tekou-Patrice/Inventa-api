package com.onescompany.Inventa.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidatedFieldTaskRepository extends JpaRepository<ValidatedFieldTask, Long> {
    List<ValidatedFieldTask> findAllByOrderByValidatedAtAscIdAsc();
    List<ValidatedFieldTask> findByReportDate(LocalDate reportDate);
    boolean existsByReportDate(LocalDate reportDate);
}
