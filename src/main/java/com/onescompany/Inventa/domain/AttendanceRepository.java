package com.onescompany.Inventa.domain;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByDate(LocalDate date);
    List<AttendanceRecord> findByDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<AttendanceRecord> findByWorkerIdAndDate(Long workerId, LocalDate date);
    List<AttendanceRecord> findByWorkerIdAndDateBetween(Long workerId, LocalDate startDate, LocalDate endDate);
    Optional<AttendanceRecord> findFirstByWorkerIdOrderByDateAsc(Long workerId);
    List<AttendanceRecord> findByStatusAndCheckOutIsNullAndCheckInLessThanEqual(
            AttendanceRecord.Status status, Instant cutoff);
}
