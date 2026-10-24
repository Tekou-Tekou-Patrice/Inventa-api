package com.onescompany.Inventa.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select worker from Worker worker where worker.id = :id")
    java.util.Optional<Worker> findByIdForUpdate(@Param("id") Long id);
}
