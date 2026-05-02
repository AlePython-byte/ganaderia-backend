package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.observability.OperationalLogSanitizer;
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
    public NotificationSendResult send(NotificationMessage notificationMessage) {
        if (notificationMessage == null) {
            return NotificationSendResult.SKIPPED;
        }

        logger.info(
                "event=notification_logged requestId={} channel={} notificationType={} severity={} metadataKeys={} metadataSize={}",
                OperationalLogSanitizer.requestId(),
                getChannel().name(),
                OperationalLogSanitizer.safe(notificationMessage.getEventType()),
                OperationalLogSanitizer.safe(notificationMessage.getSeverity()),
                OperationalLogSanitizer.metadataKeys(notificationMessage.getMetadata()),
                notificationMessage.getMetadata().size()
        );

        return NotificationSendResult.SENT;
    }
}
