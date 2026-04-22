package com.ganaderia4.backend.notification;

import java.time.LocalDateTime;
import java.util.Map;

public record WebhookNotificationPayload(
        String eventType,
        String title,
        String message,
        String severity,
        LocalDateTime createdAt,
        Map<String, String> metadata
) {

    public static WebhookNotificationPayload from(NotificationMessage notificationMessage) {
        return new WebhookNotificationPayload(
                notificationMessage.getEventType(),
                notificationMessage.getTitle(),
                notificationMessage.getMessage(),
                notificationMessage.getSeverity(),
                notificationMessage.getCreatedAt(),
                notificationMessage.getMetadata()
        );
    }
}
