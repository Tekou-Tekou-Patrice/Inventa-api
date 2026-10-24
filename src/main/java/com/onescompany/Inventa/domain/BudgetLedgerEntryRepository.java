package com.onescompany.Inventa.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetLedgerEntryRepository extends JpaRepository<BudgetLedgerEntry, Long> {
    Optional<BudgetLedgerEntry> findByIdempotencyKey(String idempotencyKey);
}
