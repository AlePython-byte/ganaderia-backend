package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.WebhookNotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "app.notifications.webhook", name = {"enabled", "processor-enabled"}, havingValue = "true")
public class WebhookNotificationDeliveryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationDeliveryProcessor.class);
    private static final String CHANNEL = NotificationChannel.WEBHOOK.name();

    private final WebhookNotificationDeliveryRepository webhookNotificationDeliveryRepository;
    private final WebhookDeliveryClient webhookDeliveryClient;
    private final DomainMetricsService domainMetricsService;
    private final WebhookNotificationProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Duration claimLeaseDuration;

    public WebhookNotificationDeliveryProcessor(WebhookNotificationDeliveryRepository webhookNotificationDeliveryRepository,
                                                WebhookDeliveryClient webhookDeliveryClient,
                                                DomainMetricsService domainMetricsService,
                                                WebhookNotificationProperties properties,
                                                PlatformTransactionManager transactionManager) {
        this.webhookNotificationDeliveryRepository = webhookNotificationDeliveryRepository;
        this.webhookDeliveryClient = webhookDeliveryClient;
        this.domainMetricsService = domainMetricsService;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.claimLeaseDuration = properties.getConnectTimeout()
                .plus(properties.getReadTimeout())
                .plusSeconds(30);
    }

    @Scheduled(fixedDelayString = "#{@webhookNotificationProperties.processorFixedDelay.toMillis()}")
    public void processDueDeliveriesScheduled() {
        processDueDeliveries();
    }

    public void processDueDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> claimedDeliveryIds = transactionTemplate.execute(status -> claimEligibleDeliveries(now));

        if (claimedDeliveryIds.isEmpty()) {
            return;
        }

        int delivered = 0;
        int retried = 0;
        int failedPermanent = 0;

        for (Long deliveryId : claimedDeliveryIds) {
            DeliveryProcessingOutcome outcome = processClaimedDelivery(deliveryId);
            if (outcome == DeliveryProcessingOutcome.DELIVERED) {
                delivered++;
            } else if (outcome == DeliveryProcessingOutcome.RETRIED) {
                retried++;
            } else if (outcome == DeliveryProcessingOutcome.FAILED_PERMANENT) {
                failedPermanent++;
            }
        }

        logger.info(
                "event=notification_webhook_processor_completed claimed={} delivered={} retried={} failedPermanent={}",
                claimedDeliveryIds.size(),
                delivered,
                retried,
                failedPermanent
        );
    }

    List<Long> claimEligibleDeliveries(LocalDateTime now) {
        validateProcessorSettings();
        Pageable page = PageRequest.of(0, properties.getProcessorBatchSize());
        List<WebhookNotificationDelivery> deliveries =
                webhookNotificationDeliveryRepository.findEligibleForProcessing(
                        List.of(WebhookNotificationDeliveryStatus.PENDING, WebhookNotificationDeliveryStatus.PROCESSING),
                        now,
                        page
                );

        if (deliveries.isEmpty()) {
            return List.of();
        }

        LocalDateTime leaseUntil = now.plus(claimLeaseDuration);
        deliveries.forEach(delivery -> delivery.markProcessing(leaseUntil));
        webhookNotificationDeliveryRepository.saveAll(deliveries);
        return deliveries.stream().map(WebhookNotificationDelivery::getId).toList();
    }

    DeliveryProcessingOutcome processClaimedDelivery(Long deliveryId) {
        ClaimedDelivery claimedDelivery = transactionTemplate.execute(status ->
                webhookNotificationDeliveryRepository.findById(deliveryId)
                        .filter(delivery -> delivery.getStatus() == WebhookNotificationDeliveryStatus.PROCESSING)
                        .map(ClaimedDelivery::from)
                        .orElse(null)
        );

        if (claimedDelivery == null) {
            return DeliveryProcessingOutcome.SKIPPED;
        }

        return processClaimedDelivery(claimedDelivery);
    }

    private DeliveryProcessingOutcome processClaimedDelivery(ClaimedDelivery delivery) {
        long startedAt = System.nanoTime();
        int attemptNumber = delivery.attempts() + 1;

        try {
            WebhookDeliveryResponse response = webhookDeliveryClient.send(delivery.toDelivery(), attemptNumber);
            return handleHttpResponse(delivery, attemptNumber, response.statusCode(), startedAt);
        } catch (HttpTimeoutException ex) {
            return handleRetryableFailure(delivery, attemptNumber, "TIMEOUT", ex.getClass().getSimpleName(), startedAt);
        } catch (IOException ex) {
            return handleRetryableFailure(delivery, attemptNumber, "IO_EXCEPTION", ex.getClass().getSimpleName(), startedAt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return handleRetryableFailure(delivery, attemptNumber, "INTERRUPTED", ex.getClass().getSimpleName(), startedAt);
        } catch (RuntimeException ex) {
            return handlePermanentFailure(
                    delivery,
                    attemptNumber,
                    "CLIENT_ERROR:" + summarizeError(ex.getMessage()),
                    ex.getClass().getSimpleName(),
                    startedAt
            );
        }
    }

    private DeliveryProcessingOutcome handleHttpResponse(ClaimedDelivery delivery,
                                                         int attemptNumber,
                                                         int statusCode,
                                                         long startedAt) {
        if (statusCode >= 200 && statusCode < 300) {
            WebhookNotificationDelivery updatedDelivery = transactionTemplate.execute(status ->
                    webhookNotificationDeliveryRepository.findById(delivery.id())
                            .map(entity -> {
                                entity.markDelivered(attemptNumber, LocalDateTime.now());
                                return webhookNotificationDeliveryRepository.save(entity);
                            })
                            .orElse(null)
            );
            domainMetricsService.incrementNotificationSent(CHANNEL, delivery.eventType());
            if (updatedDelivery != null) {
                logSuccess(updatedDelivery, statusCode, attemptNumber, startedAt);
            }
            return DeliveryProcessingOutcome.DELIVERED;
        }

        if (isRetryableStatus(statusCode)) {
            return handleRetryableStatusFailure(delivery, attemptNumber, statusCode, startedAt);
        }

        return handlePermanentFailure(
                delivery,
                attemptNumber,
                "HTTP_" + statusCode,
                "HttpStatusFailure",
                startedAt
        );
    }

    private DeliveryProcessingOutcome handleRetryableStatusFailure(ClaimedDelivery delivery,
                                                                   int attemptNumber,
                                                                   int statusCode,
                                                                   long startedAt) {
        String failureReason = "HTTP_" + statusCode;
        if (attemptNumber >= properties.getMaxAttempts()) {
            return handlePermanentFailure(
                    delivery,
                    attemptNumber,
                    failureReason,
                    "HttpStatusFailure",
                    startedAt
            );
        }

        LocalDateTime nextAttemptAt = LocalDateTime.now().plus(calculateBackoff(attemptNumber));
        WebhookNotificationDelivery updatedDelivery = transactionTemplate.execute(status ->
                webhookNotificationDeliveryRepository.findById(delivery.id())
                        .map(entity -> {
                            entity.markPendingRetry(attemptNumber, nextAttemptAt, failureReason);
                            return webhookNotificationDeliveryRepository.save(entity);
                        })
                        .orElse(null)
        );
        domainMetricsService.incrementNotificationFailed(CHANNEL, delivery.eventType());
        domainMetricsService.incrementNotificationRetried(CHANNEL, delivery.eventType());
        if (updatedDelivery != null) {
            logRetry(updatedDelivery, statusCode, attemptNumber, nextAttemptAt, "HttpStatusFailure", startedAt);
        }
        return DeliveryProcessingOutcome.RETRIED;
    }

    private DeliveryProcessingOutcome handleRetryableFailure(ClaimedDelivery delivery,
                                                             int attemptNumber,
                                                             String failureReason,
                                                             String errorType,
                                                             long startedAt) {
        if (attemptNumber >= properties.getMaxAttempts()) {
            return handlePermanentFailure(delivery, attemptNumber, failureReason, errorType, startedAt);
        }

        LocalDateTime nextAttemptAt = LocalDateTime.now().plus(calculateBackoff(attemptNumber));
        WebhookNotificationDelivery updatedDelivery = transactionTemplate.execute(status ->
                webhookNotificationDeliveryRepository.findById(delivery.id())
                        .map(entity -> {
                            entity.markPendingRetry(attemptNumber, nextAttemptAt, failureReason);
                            return webhookNotificationDeliveryRepository.save(entity);
                        })
                        .orElse(null)
        );
        domainMetricsService.incrementNotificationFailed(CHANNEL, delivery.eventType());
        domainMetricsService.incrementNotificationRetried(CHANNEL, delivery.eventType());
        if (updatedDelivery != null) {
            logRetry(updatedDelivery, "N/A", attemptNumber, nextAttemptAt, errorType, startedAt);
        }
        return DeliveryProcessingOutcome.RETRIED;
    }

    private DeliveryProcessingOutcome handlePermanentFailure(ClaimedDelivery delivery,
                                                             int attemptNumber,
                                                             String failureReason,
                                                             String errorType,
                                                             long startedAt) {
        WebhookNotificationDelivery updatedDelivery = transactionTemplate.execute(status ->
                webhookNotificationDeliveryRepository.findById(delivery.id())
                        .map(entity -> {
                            entity.markFailedPermanent(attemptNumber, LocalDateTime.now(), failureReason);
                            return webhookNotificationDeliveryRepository.save(entity);
                        })
                        .orElse(null)
        );
        domainMetricsService.incrementNotificationFailed(CHANNEL, delivery.eventType());
        if (updatedDelivery != null) {
            logPermanentFailure(updatedDelivery, failureReason, attemptNumber, errorType, startedAt);
        }
        return DeliveryProcessingOutcome.FAILED_PERMANENT;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private Duration calculateBackoff(int attemptNumber) {
        return properties.getRetryBackoff().multipliedBy(Math.max(1, attemptNumber));
    }

    private void validateProcessorSettings() {
        if (properties.getProcessorBatchSize() <= 0) {
            throw new IllegalStateException("Webhook notification processor batch-size must be greater than zero");
        }

        if (properties.getMaxAttempts() <= 0) {
            throw new IllegalStateException("Webhook notification max-attempts must be greater than zero");
        }

        if (properties.getRetryBackoff() == null
                || properties.getRetryBackoff().isZero()
                || properties.getRetryBackoff().isNegative()) {
            throw new IllegalStateException("Webhook notification retry-backoff must be greater than zero");
        }
    }

    private void logSuccess(WebhookNotificationDelivery delivery,
                            int statusCode,
                            int attemptNumber,
                            long startedAt) {
        logger.info(
                "event=notification_webhook_delivery_result channel={} result={} notificationId={} deliveryId={} destination={} status={} attempts={} durationMs={} eventType={}",
                CHANNEL,
                "delivered",
                OperationalLogSanitizer.safe(delivery.getNotificationId()),
                delivery.getId(),
                destinationSummary(delivery.getDestination()),
                statusCode,
                attemptNumber,
                elapsedMs(startedAt),
                OperationalLogSanitizer.safe(delivery.getEventType())
        );
    }

    private void logRetry(WebhookNotificationDelivery delivery,
                          Object status,
                          int attemptNumber,
                          LocalDateTime nextAttemptAt,
                          String errorType,
                          long startedAt) {
        logger.warn(
                "event=notification_webhook_delivery_result channel={} result={} notificationId={} deliveryId={} destination={} status={} attempts={} nextAttemptAt={} durationMs={} eventType={} errorType={}",
                CHANNEL,
                "retry_scheduled",
                OperationalLogSanitizer.safe(delivery.getNotificationId()),
                delivery.getId(),
                destinationSummary(delivery.getDestination()),
                status,
                attemptNumber,
                nextAttemptAt,
                elapsedMs(startedAt),
                OperationalLogSanitizer.safe(delivery.getEventType()),
                errorType
        );
    }

    private void logPermanentFailure(WebhookNotificationDelivery delivery,
                                     String failureReason,
                                     int attemptNumber,
                                     String errorType,
                                     long startedAt) {
        logger.error(
                "event=notification_webhook_delivery_result channel={} result={} notificationId={} deliveryId={} destination={} status={} attempts={} durationMs={} eventType={} errorType={} error={}",
                CHANNEL,
                "failed_permanent",
                OperationalLogSanitizer.safe(delivery.getNotificationId()),
                delivery.getId(),
                destinationSummary(delivery.getDestination()),
                delivery.getStatus().name(),
                attemptNumber,
                elapsedMs(startedAt),
                OperationalLogSanitizer.safe(delivery.getEventType()),
                errorType,
                OperationalLogSanitizer.safe(failureReason)
        );
    }

    private String summarizeError(String errorMessage) {
        return errorMessage == null || errorMessage.isBlank() ? "UNKNOWN" : errorMessage;
    }

    private String destinationSummary(String destination) {
        try {
            return OperationalLogSanitizer.destination(URI.create(destination));
        } catch (RuntimeException ex) {
            return OperationalLogSanitizer.safe(destination);
        }
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    enum DeliveryProcessingOutcome {
        DELIVERED,
        RETRIED,
        FAILED_PERMANENT,
        SKIPPED
    }

    record ClaimedDelivery(Long id,
                           String notificationId,
                           String eventType,
                           String payload,
                           String destination,
                           int attempts) {

        static ClaimedDelivery from(WebhookNotificationDelivery delivery) {
            return new ClaimedDelivery(
                    delivery.getId(),
                    delivery.getNotificationId(),
                    delivery.getEventType(),
                    delivery.getPayload(),
                    delivery.getDestination(),
                    delivery.getAttempts()
            );
        }

        WebhookNotificationDelivery toDelivery() {
            WebhookNotificationDelivery delivery = new WebhookNotificationDelivery();
            delivery.setId(id);
            delivery.setNotificationId(notificationId);
            delivery.setEventType(eventType);
            delivery.setPayload(payload);
            delivery.setDestination(destination);
            delivery.setAttempts(attempts);
            return delivery;
        }
    }
}
