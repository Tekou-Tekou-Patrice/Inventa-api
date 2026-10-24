package com.onescompany.Inventa.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantationFieldRepository extends JpaRepository<PlantationField, Long> {
    boolean existsByNameIgnoreCase(String name);
}
