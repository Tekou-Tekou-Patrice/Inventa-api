package com.onescompany.Inventa.domain;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyReportValidationRepository extends JpaRepository<DailyReportValidation, LocalDate> {
}
