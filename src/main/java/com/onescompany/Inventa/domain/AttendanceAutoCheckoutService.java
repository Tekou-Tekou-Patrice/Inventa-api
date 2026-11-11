package com.onescompany.Inventa.domain;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceAutoCheckoutService {
    private final AttendanceRepository attendance;
    private final DailyReportValidationRepository reportValidations;

    public AttendanceAutoCheckoutService(
            AttendanceRepository attendance,
            DailyReportValidationRepository reportValidations) {
        this.attendance = attendance;
        this.reportValidations = reportValidations;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void closeExpiredSessions() {
        Instant now = Instant.now();
        attendance.findByStatusAndCheckOutIsNullAndCheckInLessThanEqual(
                        AttendanceRecord.Status.PRESENT, now.minusSeconds(5 * 60 * 60))
                .forEach(record -> {
                    if (record.closeExpiredSession(now, "Automatique")) {
                        reportValidations.deleteById(record.getDate());
                    }
                });
    }
}
