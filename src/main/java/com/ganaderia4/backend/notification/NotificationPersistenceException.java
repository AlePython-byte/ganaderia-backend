package com.ganaderia4.backend.notification;

public class NotificationPersistenceException extends RuntimeException {

    public NotificationPersistenceException(String message) {
        super(message);
    }

    public NotificationPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
