package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class AlertResponseDTO {

    private Long id;
    private String type;
    private String message;
    private LocalDateTime createdAt;
    private String status;
    private String observations;
    private Long cowId;
    private String cowIdentifier;
    private String cowName;
    private Long locationId;

    public AlertResponseDTO() {
    }

    public AlertResponseDTO(Long id, String type, String message, LocalDateTime createdAt,
                            String status, String observations, Long cowId,
                            String cowIdentifier, String cowName, Long locationId) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.status = status;
        this.observations = observations;
        this.cowId = cowId;
        this.cowIdentifier = cowIdentifier;
        this.cowName = cowName;
        this.locationId = locationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Long getCowId() {
        return cowId;
    }

    public void setCowId(Long cowId) {
        this.cowId = cowId;
    }

    public String getCowIdentifier() {
        return cowIdentifier;
    }

    public void setCowIdentifier(String cowIdentifier) {
        this.cowIdentifier = cowIdentifier;
    }

    public String getCowName() {
        return cowName;
    }

    public void setCowName(String cowName) {
        this.cowName = cowName;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}