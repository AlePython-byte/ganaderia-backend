package com.ganaderia4.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.LOG;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {
        if (notificationMessage == null) {
            return;
        }

        logger.warn(
                "NOTIFICATION channel={} eventType={} severity={} title={} message={} metadata={}",
                getChannel().name(),
                notificationMessage.getEventType(),
                notificationMessage.getSeverity(),
                notificationMessage.getTitle(),
                notificationMessage.getMessage(),
                notificationMessage.getMetadata()
        );
    }
}