package com.onescompany.Inventa.domain;

import java.time.Instant;

import jakarta.persistence.Embeddable;

@Embeddable
public class AttendanceSession {
    private Instant checkIn;
    private Instant checkOut;

    protected AttendanceSession() {
    }

    public AttendanceSession(Instant checkIn, Instant checkOut) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public void close(Instant checkOut) {
        this.checkOut = checkOut;
    }

    public Instant getCheckIn() { return checkIn; }
    public Instant getCheckOut() { return checkOut; }
}
