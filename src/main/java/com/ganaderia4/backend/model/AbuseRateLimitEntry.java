package com.ganaderia4.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "abuse_rate_limits",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_abuse_rate_limits_scope_key",
                columnNames = {"scope", "abuse_key"}
        )
)
public class AbuseRateLimitEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(name = "abuse_key", nullable = false, length = 256)
    private String abuseKey;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

    public String getAbuseKey() {
        return abuseKey;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setAbuseKey(String abuseKey) {
        this.abuseKey = abuseKey;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setBlockedUntil(Instant blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
