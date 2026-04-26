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
    private String cowToken;
    private String cowName;
    private Long locationId;
    private Integer priorityScore;
    private String priority;

    public AlertResponseDTO() {
    }

    public AlertResponseDTO(Long id, String type, String message, LocalDateTime createdAt,
                            String status, String observations, Long cowId,
                            String cowToken, String cowName, Long locationId) {
        this(id, type, message, createdAt, status, observations, cowId, cowToken, cowName, locationId, null, null);
    }

    public AlertResponseDTO(Long id, String type, String message, LocalDateTime createdAt,
                            String status, String observations, Long cowId,
                            String cowToken, String cowName, Long locationId,
                            Integer priorityScore, String priority) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.status = status;
        this.observations = observations;
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
        this.locationId = locationId;
        this.priorityScore = priorityScore;
        this.priority = priority;
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

    public String getCowToken() {
        return cowToken;
    }

    public void setCowToken(String cowToken) {
        this.cowToken = cowToken;
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

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
