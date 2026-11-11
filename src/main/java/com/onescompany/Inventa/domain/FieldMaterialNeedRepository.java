package com.onescompany.Inventa.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldMaterialNeedRepository extends JpaRepository<FieldMaterialNeed, Long> {
    List<FieldMaterialNeed> findTop100ByOrderByCreatedAtDesc();
}
