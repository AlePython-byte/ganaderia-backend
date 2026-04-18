package com.ganaderia4.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationDispatcher.class);

    private final List<NotificationService> notificationServices;

    public DefaultNotificationDispatcher(List<NotificationService> notificationServices) {
        this.notificationServices = notificationServices;
    }

    @Override
    public void dispatch(NotificationMessage notificationMessage) {
        if (notificationMessage == null || notificationServices == null || notificationServices.isEmpty()) {
            return;
        }

        for (NotificationService notificationService : notificationServices) {
            try {
                notificationService.send(notificationMessage);
            } catch (RuntimeException ex) {
                logger.error(
                        "Error dispatching notification channel={} eventType={} severity={} error={}",
                        resolveChannel(notificationService),
                        notificationMessage.getEventType(),
                        notificationMessage.getSeverity(),
                        ex.getMessage()
                );
            }
        }
    }

    private String resolveChannel(NotificationService notificationService) {
        if (notificationService == null || notificationService.getChannel() == null) {
            return "UNKNOWN";
        }

        return notificationService.getChannel().name();
    }
}
