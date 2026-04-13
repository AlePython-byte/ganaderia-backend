package com.ganaderia4.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String entityType;

    private Long entityId;

    @Column(length = 255)
    private String actor;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean success;

    public AuditLog() {
    }

    public AuditLog(Long id,
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