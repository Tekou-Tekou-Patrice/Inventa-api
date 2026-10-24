package com.onescompany.Inventa.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OperatingBudgetRepository extends JpaRepository<OperatingBudget, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select budget from OperatingBudget budget where budget.id = :id")
    Optional<OperatingBudget> findByIdForUpdate(@Param("id") Long id);
}
