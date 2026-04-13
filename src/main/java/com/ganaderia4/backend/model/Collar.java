package com.ganaderia4.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "collars")
public class Collar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollarStatus status;

    @OneToOne
    @JoinColumn(name = "cow_id", unique = true)
    private Cow cow;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_status")
    private DeviceSignalStatus signalStatus;

    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Column(name = "gps_accuracy")
    private Double gpsAccuracy;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Collar() {
    }

    public Collar(Long id,
                  String token,
                  CollarStatus status,
                  Cow cow,
                  Integer batteryLevel,
                  LocalDateTime lastSeenAt,
                  DeviceSignalStatus signalStatus,
                  String firmwareVersion,
                  Double gpsAccuracy,
                  Boolean enabled,
                  String notes) {
        this.id = id;
        this.token = token;
        this.status = status;
        this.cow = cow;
        this.batteryLevel = batteryLevel;
        this.lastSeenAt = lastSeenAt;
        this.signalStatus = signalStatus;
        this.firmwareVersion = firmwareVersion;
        this.gpsAccuracy = gpsAccuracy;
        this.enabled = enabled;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CollarStatus getStatus() {
        return status;
    }

    public void setStatus(CollarStatus status) {
        this.status = status;
    }

    public Cow getCow() {
        return cow;
    }

    public void setCow(Cow cow) {
        this.cow = cow;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public DeviceSignalStatus getSignalStatus() {
        return signalStatus;
    }

    public void setSignalStatus(DeviceSignalStatus signalStatus) {
        this.signalStatus = signalStatus;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public Double getGpsAccuracy() {
        return gpsAccuracy;
    }

    public void setGpsAccuracy(Double gpsAccuracy) {
        this.gpsAccuracy = gpsAccuracy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}