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
        name = "device_replay_nonces",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_device_replay_nonces_device_nonce",
                columnNames = {"device_token", "nonce"}
        )
)
public class DeviceReplayNonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_token", nullable = false, length = 100)
    private String deviceToken;

    @Column(nullable = false, length = 128)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DeviceReplayNonce() {
    }

    public DeviceReplayNonce(String deviceToken, String nonce, Instant expiresAt, Instant createdAt) {
        this.deviceToken = deviceToken;
        this.nonce = nonce;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getNonce() {
        return nonce;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
