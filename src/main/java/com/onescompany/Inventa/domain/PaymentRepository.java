package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findTop100ByOrderByRecordedAtDesc();
    List<Payment> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Instant start, Instant end);
    Optional<Payment> findFirstByWorkerIdAndPaidThroughEndIsNotNullOrderByPaidThroughEndDesc(Long workerId);
}
