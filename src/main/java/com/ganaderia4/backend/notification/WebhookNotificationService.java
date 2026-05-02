package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.WebhookNotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.notifications.webhook.enabled", havingValue = "true")
public class WebhookNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationService.class);

    private final ObjectMapper objectMapper;
    private final WebhookNotificationDeliveryRepository webhookNotificationDeliveryRepository;
    private final DomainMetricsService domainMetricsService;
    private final URI webhookUri;
    private final Duration readTimeout;

    public WebhookNotificationService(WebhookNotificationProperties properties,
                                      ObjectMapper objectMapper,
                                      WebhookNotificationDeliveryRepository webhookNotificationDeliveryRepository,
                                      DomainMetricsService domainMetricsService) {
        this.objectMapper = objectMapper;
        this.webhookNotificationDeliveryRepository = webhookNotificationDeliveryRepository;
        this.domainMetricsService = domainMetricsService;
        this.webhookUri = validateWebhookUri(properties.getUrl());
        validateTimeout(properties.getConnectTimeout(), "connect-timeout");
        this.readTimeout = validateTimeout(properties.getReadTimeout(), "read-timeout");
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WEBHOOK;
    }

    @Override
    public NotificationSendResult send(NotificationMessage notificationMessage) {
        if (notificationMessage == null) {
            return NotificationSendResult.SKIPPED;
        }

        try {
            String requestBody = serialize(notificationMessage);
            WebhookNotificationDelivery delivery = WebhookNotificationDelivery.pending(
                    notificationMessage.getEventType(),
                    requestBody,
                    webhookUri.toString()
            );

            WebhookNotificationDelivery savedDelivery = webhookNotificationDeliveryRepository.save(delivery);
            domainMetricsService.incrementNotificationQueued(getChannel().name(), notificationMessage.getEventType());
            logQueued(savedDelivery, notificationMessage);
            return NotificationSendResult.SENT;
        } catch (NotificationPersistenceException ex) {
            logPersistenceFailure(notificationMessage, ex);
            throw ex;
        } catch (RuntimeException ex) {
            logPersistenceFailure(notificationMessage, ex);
            throw new NotificationPersistenceException("Webhook notification delivery could not be persisted", ex);
        }
    }

    private String serialize(NotificationMessage notificationMessage) {
        try {
            return objectMapper.writeValueAsString(WebhookNotificationPayload.from(notificationMessage));
        } catch (JsonProcessingException ex) {
            throw new NotificationPersistenceException("Webhook notification payload could not be serialized", ex);
        }
    }

    private URI validateWebhookUri(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Webhook notification URL must be configured when webhook channel is enabled");
        }

        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("Webhook notification URL must use http or https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("Webhook notification URL must include a host");
        }

        return uri;
    }

    private Duration validateTimeout(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("Webhook notification " + propertyName + " must be greater than zero");
        }

        return timeout;
    }

    private void logQueued(WebhookNotificationDelivery delivery,
                           NotificationMessage notificationMessage) {
        logger.info(
                "event=webhook_delivery_enqueued requestId={} notificationId={} host={} status={} attempts={} nextAttemptAt={} readTimeoutMs={} notificationType={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(delivery.getNotificationId()),
                OperationalLogSanitizer.destination(webhookUri),
                delivery.getStatus().name(),
                delivery.getAttempts(),
                delivery.getNextAttemptAt(),
                readTimeout.toMillis(),
                OperationalLogSanitizer.safe(notificationMessage.getEventType())
        );
    }

    private void logPersistenceFailure(NotificationMessage notificationMessage,
                                       RuntimeException exception) {
        logger.error(
                "event=webhook_delivery_enqueue_failed requestId={} host={} notificationType={} errorType={} error={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.destination(webhookUri),
                OperationalLogSanitizer.safe(notificationMessage.getEventType()),
                exception.getClass().getSimpleName(),
                OperationalLogSanitizer.safe(exception.getMessage())
        );
    }
}
