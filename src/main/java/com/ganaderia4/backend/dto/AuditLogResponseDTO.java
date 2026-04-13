package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class AuditLogResponseDTO {

    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String actor;
    private String source;
    private String details;
    private LocalDateTime createdAt;
    private Boolean success;

    public AuditLogResponseDTO() {
    }

    public AuditLogResponseDTO(Long id,
                               String action,
                               String entityType,
                               Long entityId,
                               String actor,
                               String source,
                               String details,
                               LocalDateTime createdAt,
                               Boolean success) {
        this.id = id;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actor = actor;
        this.source = source;
        this.details = details;
        this.createdAt = createdAt;
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getActor() {
        return actor;
    }

    public String getSource() {
        return source;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}