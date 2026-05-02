package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationDispatcher.class);

    private final List<NotificationService> notificationServices;
    private final DomainMetricsService domainMetricsService;

    public DefaultNotificationDispatcher(List<NotificationService> notificationServices,
                                         DomainMetricsService domainMetricsService) {
        this.notificationServices = notificationServices.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(this::priority))
                .toList();
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
                NotificationSendResult result = notificationService.send(notificationMessage);
                if (result == NotificationSendResult.SENT
                        && notificationService.getChannel() != NotificationChannel.WEBHOOK) {
                    domainMetricsService.incrementNotificationSent(channel, notificationMessage.getEventType());
                }
            } catch (NotificationPersistenceException ex) {
                domainMetricsService.incrementNotificationFailed(channel, notificationMessage.getEventType());
                logFailure(channel, notificationMessage, ex);
                throw ex;
            } catch (RuntimeException ex) {
                domainMetricsService.incrementNotificationFailed(channel, notificationMessage.getEventType());
                logFailure(channel, notificationMessage, ex);
            }
        }
    }

    private void logFailure(String channel, NotificationMessage notificationMessage, RuntimeException ex) {
        logger.error(
                "event=notification_dispatch_failed requestId={} channel={} notificationType={} severity={} errorType={} error={}",
                OperationalLogSanitizer.requestId(),
                channel,
                OperationalLogSanitizer.safe(notificationMessage.getEventType()),
                OperationalLogSanitizer.safe(notificationMessage.getSeverity()),
                ex.getClass().getSimpleName(),
                OperationalLogSanitizer.safe(ex.getMessage())
        );
    }

    private String resolveChannel(NotificationService notificationService) {
        if (notificationService == null || notificationService.getChannel() == null) {
            return "UNKNOWN";
        }

        return notificationService.getChannel().name();
    }

    private int priority(NotificationService notificationService) {
        if (notificationService == null || notificationService.getChannel() == null) {
            return 2;
        }

        if (notificationService.getChannel() == NotificationChannel.WEBHOOK) {
            return 0;
        }

        return 1;
    }
}
