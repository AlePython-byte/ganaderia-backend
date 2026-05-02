package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.model.NotificationSeverity;

import java.util.List;

public record EmailNotificationRecipientsResolution(
        List<String> recipients,
        NotificationSeverity severity,
        boolean globalFallbackUsed
) {
}
