package com.onescompany.Inventa.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "attendance_records", uniqueConstraints = @UniqueConstraint(columnNames = { "worker_id", "work_date" }))
public class AttendanceRecord {
    public enum Status {
        PRESENT,
        ABSENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status;

    private Instant checkIn;

    private Instant checkOut;

    @ElementCollection
    @CollectionTable(name = "attendance_sessions", joinColumns = @JoinColumn(name = "attendance_record_id"))
    @OrderColumn(name = "session_order")
    private List<AttendanceSession> sessions = new ArrayList<>();

    @Column(nullable = false, length = 80)
    private String recordedBy;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(
            Worker worker, LocalDate date, Status status, Instant checkIn, Instant checkOut, String recordedBy) {
        this.worker = worker;
        this.date = date;
        this.status = status;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.recordedBy = recordedBy;
        if (status == Status.PRESENT && checkIn != null) {
            sessions.add(new AttendanceSession(checkIn, checkOut));
        }
    }

    public void update(Status status, Instant checkIn, Instant checkOut, String recordedBy) {
        this.status = status;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.recordedBy = recordedBy;
        sessions.clear();
        if (status == Status.PRESENT && checkIn != null) {
            sessions.add(new AttendanceSession(checkIn, checkOut));
        }
    }

    public void startSession(Instant checkIn, String recordedBy) {
        ensureLegacySession();
        this.status = Status.PRESENT;
        this.checkIn = checkIn;
        this.checkOut = null;
        this.recordedBy = recordedBy;
        sessions.add(new AttendanceSession(checkIn, null));
    }

    public boolean closeSession(Instant checkOut, String recordedBy) {
        ensureLegacySession();
        if (sessions.isEmpty()) {
            return false;
        }
        AttendanceSession session = sessions.get(sessions.size() - 1);
        if (session.getCheckOut() != null || checkOut.isBefore(session.getCheckIn())) {
            return false;
        }
        session.close(checkOut);
        this.checkOut = checkOut;
        this.recordedBy = recordedBy;
        return true;
    }

    public boolean closeExpiredSession(Instant now, String recordedBy) {
        ensureLegacySession();
        if (sessions.isEmpty()) {
            return false;
        }
        AttendanceSession latest = sessions.get(sessions.size() - 1);
        if (latest.getCheckOut() != null) {
            return false;
        }
        Instant deadline = latest.getCheckIn().plusSeconds(5 * 60 * 60);
        return !now.isBefore(deadline) && closeSession(deadline, recordedBy);
    }

    public List<AttendanceSession> getSessions() {
        ensureLegacySession();
        return List.copyOf(sessions);
    }

    public long workedMinutes() {
        return getSessions().stream()
                .filter(session -> session.getCheckIn() != null && session.getCheckOut() != null)
                .mapToLong(session -> java.time.Duration.between(
                        session.getCheckIn(), session.getCheckOut()).toMinutes())
                .filter(minutes -> minutes > 0)
                .sum();
    }

    private void ensureLegacySession() {
        if (sessions.isEmpty() && checkIn != null) {
            sessions.add(new AttendanceSession(checkIn, checkOut));
        }
    }

    public Long getId() { return id; }
    public Worker getWorker() { return worker; }
    public LocalDate getDate() { return date; }
    public Status getStatus() { return status; }
    public Instant getCheckIn() { return checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public String getRecordedBy() { return recordedBy; }
}
