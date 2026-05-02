package com.ganaderia4.backend.notification;

public record EmailNotificationRequest(
        String from,
        String to,
        String subject,
        String textBody
) {
}
