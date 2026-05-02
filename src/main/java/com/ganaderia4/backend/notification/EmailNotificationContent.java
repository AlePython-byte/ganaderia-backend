package com.ganaderia4.backend.notification;

public record EmailNotificationContent(
        String subject,
        String textBody,
        String htmlBody
) {
}
