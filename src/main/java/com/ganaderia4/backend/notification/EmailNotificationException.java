package com.ganaderia4.backend.notification;

public class EmailNotificationException extends RuntimeException {

    public EmailNotificationException(String message) {
        super(message);
    }

    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
