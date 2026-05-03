package com.ganaderia4.backend.notification;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    DEAD
}
