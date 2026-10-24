package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PayrollPaymentRequestRepository extends JpaRepository<PayrollPaymentRequest, Long> {
    List<PayrollPaymentRequest> findTop100ByOrderByRequestedAtDesc();

    boolean existsByWorker_IdAndStatusIn(
            Long workerId,
            java.util.Collection<PayrollPaymentRequest.Status> statuses);

    Optional<PayrollPaymentRequest> findFirstByWorker_IdAndStatusOrderByRequestedAtDesc(
            Long workerId,
            PayrollPaymentRequest.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from PayrollPaymentRequest request join fetch request.worker where request.id = :id")
    Optional<PayrollPaymentRequest> findByIdForUpdate(@Param("id") Long id);
}
