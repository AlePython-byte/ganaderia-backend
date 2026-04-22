package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationDispatcher.class);

    private final List<NotificationService> notificationServices;
    private final DomainMetricsService domainMetricsService;

    public DefaultNotificationDispatcher(List<NotificationService> notificationServices,
                                         DomainMetricsService domainMetricsService) {
        this.notificationServices = List.copyOf(notificationServices);
        this.domainMetricsService = domainMetricsService;
    }

    @Override
    public void dispatch(NotificationMessage notificationMessage) {
        if (notificationMessage == null || notificationServices.isEmpty()) {
            return;
        }

        for (NotificationService notificationService : notificationServices) {
            String channel = resolveChannel(notificationService);
            try {
                notificationService.send(notificationMessage);
                domainMetricsService.incrementNotificationSent(channel, notificationMessage.getEventType());
            } catch (RuntimeException ex) {
                domainMetricsService.incrementNotificationFailed(channel, notificationMessage.getEventType());
                logger.error(
                        "event=notification_dispatch_failed channel={} eventType={} severity={} errorType={} error={}",
                        channel,
                        OperationalLogSanitizer.safe(notificationMessage.getEventType()),
                        OperationalLogSanitizer.safe(notificationMessage.getSeverity()),
                        ex.getClass().getSimpleName(),
                        OperationalLogSanitizer.safe(ex.getMessage())
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
