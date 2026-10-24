package com.onescompany.Inventa.security;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {
    Optional<PushDeviceToken> findByToken(String token);

    List<PushDeviceToken> findByUsername(String username);

    List<PushDeviceToken> findByUsernameIn(List<String> usernames);

    void deleteByToken(String token);
}
