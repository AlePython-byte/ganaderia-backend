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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "webhook_notification_deliveries",
        indexes = {
                @Index(name = "idx_webhook_notification_deliveries_status_next_attempt", columnList = "status,next_attempt_at"),
                @Index(name = "idx_webhook_notification_deliveries_created_at", columnList = "created_at")
        }
)
public class WebhookNotificationDelivery {

    private static final int MAX_EVENT_TYPE_LENGTH = 100;
    private static final int MAX_DESTINATION_LENGTH = 500;
    private static final int MAX_LAST_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false, length = 36, unique = true)
    private String notificationId;

    @Column(name = "event_type", nullable = false, length = MAX_EVENT_TYPE_LENGTH)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = MAX_DESTINATION_LENGTH)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookNotificationDeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = MAX_LAST_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WebhookNotificationDelivery() {
    }

    public static WebhookNotificationDelivery pending(String eventType, String payload, String destination) {
        LocalDateTime now = LocalDateTime.now();

        WebhookNotificationDelivery delivery = new WebhookNotificationDelivery();
        delivery.setNotificationId(UUID.randomUUID().toString());
        delivery.setEventType(eventType);
        delivery.setPayload(payload);
        delivery.setDestination(destination);
        delivery.setStatus(WebhookNotificationDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setNextAttemptAt(now);
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);
        return delivery;
    }

    public void markProcessing(LocalDateTime leaseUntil) {
        setStatus(WebhookNotificationDeliveryStatus.PROCESSING);
        setNextAttemptAt(leaseUntil);
    }

    public void markDelivered(int attemptsUsed, LocalDateTime processedAt) {
        setAttempts(attemptsUsed);
        setStatus(WebhookNotificationDeliveryStatus.DELIVERED);
        setNextAttemptAt(processedAt);
        setLastError(null);
    }

    public void markPendingRetry(int attemptsUsed, LocalDateTime nextAttemptAt, String lastError) {
        setAttempts(attemptsUsed);
        setStatus(WebhookNotificationDeliveryStatus.PENDING);
        setNextAttemptAt(nextAttemptAt);
        setLastError(lastError);
    }

    public void markFailedPermanent(int attemptsUsed, LocalDateTime processedAt, String lastError) {
        setAttempts(attemptsUsed);
        setStatus(WebhookNotificationDeliveryStatus.FAILED_PERMANENT);
        setNextAttemptAt(processedAt);
        setLastError(lastError);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (notificationId == null || notificationId.isBlank()) {
            notificationId = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = WebhookNotificationDeliveryStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getDestination() {
        return destination;
    }

    public WebhookNotificationDeliveryStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = truncate(notificationId, 36);
    }

    public void setEventType(String eventType) {
        this.eventType = truncate(blankToDefault(eventType, "UNKNOWN"), MAX_EVENT_TYPE_LENGTH);
    }

    public void setPayload(String payload) {
        this.payload = blankToDefault(payload, "{}");
    }

    public void setDestination(String destination) {
        this.destination = truncate(blankToDefault(destination, "UNKNOWN"), MAX_DESTINATION_LENGTH);
    }

    public void setStatus(WebhookNotificationDeliveryStatus status) {
        this.status = status != null ? status : WebhookNotificationDeliveryStatus.PENDING;
    }

    public void setAttempts(int attempts) {
        this.attempts = Math.max(0, attempts);
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public void setLastError(String lastError) {
        this.lastError = truncate(lastError, MAX_LAST_ERROR_LENGTH);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
