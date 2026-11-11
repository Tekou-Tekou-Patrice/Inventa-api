package com.onescompany.Inventa.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FieldTaskAdvanceRepository extends JpaRepository<FieldTaskAdvance, Long> {
    List<FieldTaskAdvance> findByTaskIdOrderByPaidAtAscIdAsc(Long taskId);
    Optional<FieldTaskAdvance> findByPaidByAndIdempotencyKey(String paidBy, String idempotencyKey);

    @Query("select coalesce(sum(a.amount), 0) from FieldTaskAdvance a where a.task.id = :taskId")
    long sumAmountByTaskId(Long taskId);
}
