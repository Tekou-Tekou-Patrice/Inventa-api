package com.onescompany.Inventa.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "director_registration_gate")
public class DirectorRegistrationGate {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    protected DirectorRegistrationGate() {
    }
}
