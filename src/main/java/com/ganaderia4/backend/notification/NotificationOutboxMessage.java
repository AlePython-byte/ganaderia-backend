package com.ganaderia4.backend.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "notification_outbox",
        indexes = {
                @Index(name = "idx_notification_outbox_status_next_attempt", columnList = "status,next_attempt_at"),
                @Index(name = "idx_notification_outbox_created_at", columnList = "created_at")
        }
)
public class NotificationOutboxMessage {

    static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int MAX_EVENT_TYPE_LENGTH = 100;
    private static final int MAX_RECIPIENT_LENGTH = 255;
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_LAST_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationOutboxStatus status;

    @Column(name = "event_type", nullable = false, length = MAX_EVENT_TYPE_LENGTH)
    private String eventType;

    @Column(length = MAX_RECIPIENT_LENGTH)
    private String recipient;

    @Column(length = MAX_SUBJECT_LENGTH)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error", length = MAX_LAST_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (status == null) {
            status = NotificationOutboxStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
        if (maxAttempts <= 0) {
            maxAttempts = DEFAULT_MAX_ATTEMPTS;
        }
        attempts = Math.max(0, attempts);
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationOutboxStatus getStatus() {
        return status;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getLastError() {
        return lastError;
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

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public void setStatus(NotificationOutboxStatus status) {
        this.status = status != null ? status : NotificationOutboxStatus.PENDING;
    }

    public void setEventType(String eventType) {
        this.eventType = truncate(trimToNull(eventType), MAX_EVENT_TYPE_LENGTH);
    }

    public void setRecipient(String recipient) {
        this.recipient = truncate(trimToNull(recipient), MAX_RECIPIENT_LENGTH);
    }

    public void setSubject(String subject) {
        this.subject = truncate(trimToNull(subject), MAX_SUBJECT_LENGTH);
    }

    public void setPayload(String payload) {
        this.payload = trimToNull(payload);
    }

    public void setAttempts(int attempts) {
        this.attempts = Math.max(0, attempts);
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : DEFAULT_MAX_ATTEMPTS;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public void setLastError(String lastError) {
        this.lastError = truncate(trimToNull(lastError), MAX_LAST_ERROR_LENGTH);
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
