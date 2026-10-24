package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface FieldBudgetAllocationRepository extends JpaRepository<FieldBudgetAllocation, Long> {
    @Query("select allocation from FieldBudgetAllocation allocation "
            + "join fetch allocation.field where allocation.field.id = :fieldId")
    Optional<FieldBudgetAllocation> findByFieldId(@Param("fieldId") Long fieldId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select allocation from FieldBudgetAllocation allocation "
            + "join fetch allocation.field where allocation.field.id = :fieldId")
    Optional<FieldBudgetAllocation> findByFieldIdForUpdate(@Param("fieldId") Long fieldId);

    List<FieldBudgetAllocation> findAllByOrderByIdAsc();
}
