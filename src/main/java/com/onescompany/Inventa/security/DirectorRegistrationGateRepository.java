package com.onescompany.Inventa.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DirectorRegistrationGateRepository extends JpaRepository<DirectorRegistrationGate, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select gate from DirectorRegistrationGate gate where gate.id = :id")
    Optional<DirectorRegistrationGate> findByIdForUpdate(@Param("id") Long id);
}
