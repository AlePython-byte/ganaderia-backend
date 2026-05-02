package com.ganaderia4.backend.notification;

import java.util.List;

public record EmailNotificationRequest(
        String from,
        List<String> to,
        String subject,
        String textBody,
        String htmlBody
) {
}
