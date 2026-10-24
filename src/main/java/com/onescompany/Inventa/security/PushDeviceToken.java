package com.onescompany.Inventa.security;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "push_device_tokens")
public class PushDeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 16)
    private String platform;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected PushDeviceToken() {
    }

    public PushDeviceToken(String token, String username, String platform) {
        update(token, username, platform);
    }

    public void update(String token, String username, String platform) {
        this.token = token;
        this.username = username;
        this.platform = platform;
        this.updatedAt = Instant.now();
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
