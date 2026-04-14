package com.ganaderia4.backend.notification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NotificationMessage {

    private final String eventType;
    private final String title;
    private final String message;
    private final String severity;
    private final LocalDateTime createdAt;
    private final Map<String, String> metadata;

    public NotificationMessage(String eventType,
                               String title,
                               String message,
                               String severity,
                               LocalDateTime createdAt,
                               Map<String, String> metadata) {
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.createdAt = createdAt;
        this.metadata = metadata != null
                ? Collections.unmodifiableMap(new HashMap<>(metadata))
                : Collections.emptyMap();
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getSeverity() {
        return severity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventType;
        private String title;
        private String message;
        private String severity;
        private LocalDateTime createdAt;
        private final Map<String, String> metadata = new HashMap<>();

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder metadata(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public NotificationMessage build() {
            return new NotificationMessage(
                    eventType,
                    title,
                    message,
                    severity,
                    createdAt != null ? createdAt : LocalDateTime.now(),
                    metadata
            );
        }
    }
}