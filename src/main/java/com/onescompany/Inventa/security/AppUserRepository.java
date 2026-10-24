package com.onescompany.Inventa.security;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(UserRole role);

    @Query("""
            select user.username from AppUser user
            where user.enabled = true and user.role = :role
              and (:fieldId is null or user.field.id = :fieldId)
            """)
    List<String> findEnabledUsernamesByRoleAndField(
            @Param("role") UserRole role,
            @Param("fieldId") Long fieldId);
}
