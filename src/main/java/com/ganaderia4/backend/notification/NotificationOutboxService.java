package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;

@Service
public class NotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxService.class);

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final Clock clock;

    public NotificationOutboxService(NotificationOutboxRepository notificationOutboxRepository,
                                     Clock clock) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.clock = clock;
    }

    @Transactional
    public NotificationOutboxMessage enqueue(NotificationChannel channel,
                                             String eventType,
                                             String recipient,
                                             String subject,
                                             String payload) {
        validate(channel, eventType, payload);

        Instant now = Instant.now(clock);
        NotificationOutboxMessage message = new NotificationOutboxMessage();
        message.setChannel(channel);
        message.setStatus(NotificationOutboxStatus.PENDING);
        message.setEventType(eventType);
        message.setRecipient(recipient);
        message.setSubject(subject);
        message.setPayload(payload);
        message.setAttempts(0);
        message.setMaxAttempts(NotificationOutboxMessage.DEFAULT_MAX_ATTEMPTS);
        message.setNextAttemptAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        NotificationOutboxMessage saved = notificationOutboxRepository.save(message);
        log.info(
                "event=notification_outbox_enqueued requestId={} channel={} eventType={} messageId={}",
                OperationalLogSanitizer.requestIdOr("scheduled"),
                channel.name(),
                OperationalLogSanitizer.safe(saved.getEventType()),
                saved.getId()
        );
        return saved;
    }

    private void validate(NotificationChannel channel, String eventType, String payload) {
        if (channel == null) {
            throw new IllegalArgumentException("Notification channel is required");
        }
        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("Notification event type is required");
        }
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("Notification payload is required");
        }
    }
}
