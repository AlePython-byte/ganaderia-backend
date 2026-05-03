package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.config.NotificationOutboxEmailProcessorProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NotificationOutboxEmailProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxEmailProcessor.class);

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxEmailProcessorProperties properties;
    private final EmailNotificationProperties emailNotificationProperties;
    private final Map<String, EmailProviderClient> providerClients;
    private final DomainMetricsService domainMetricsService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationOutboxEmailProcessor(NotificationOutboxRepository notificationOutboxRepository,
                                            NotificationOutboxEmailProcessorProperties properties,
                                            EmailNotificationProperties emailNotificationProperties,
                                            List<EmailProviderClient> providerClients,
                                            DomainMetricsService domainMetricsService,
                                            ObjectMapper objectMapper,
                                            Clock clock) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.properties = properties;
        this.emailNotificationProperties = emailNotificationProperties;
        this.providerClients = providerClients.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        client -> normalize(client.getProviderName()),
                        client -> client
                ));
        this.domainMetricsService = domainMetricsService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "#{@notificationOutboxEmailProcessorProperties.processorFixedDelay.toMillis()}")
    @Transactional
    public void processPendingEmailMessages() {
        if (!properties.isProcessorEnabled() || !emailNotificationProperties.isEnabled()) {
            return;
        }

        long startedAt = System.nanoTime();
        String requestId = OperationalLogSanitizer.requestIdOr("scheduled");
        int processed = 0;
        int sent = 0;
        int failed = 0;
        int dead = 0;

        try {
            Instant now = Instant.now(clock);
            List<NotificationOutboxMessage> messages = notificationOutboxRepository.findEligibleForProcessing(
                    NotificationChannel.EMAIL,
                    List.of(NotificationOutboxStatus.PENDING, NotificationOutboxStatus.FAILED),
                    now,
                    PageRequest.of(0, effectiveBatchSize())
            );

            for (NotificationOutboxMessage message : messages) {
                processed++;
                ProcessingOutcome outcome = processSingleMessage(message, now, requestId);
                if (outcome == ProcessingOutcome.SENT) {
                    sent++;
                } else if (outcome == ProcessingOutcome.FAILED) {
                    failed++;
                } else if (outcome == ProcessingOutcome.DEAD) {
                    dead++;
                }
            }

            if (sent > 0) {
                domainMetricsService.incrementNotificationOutboxEmailSent(sent);
            }
            if (failed > 0) {
                domainMetricsService.incrementNotificationOutboxEmailFailed(failed);
            }
            if (dead > 0) {
                domainMetricsService.incrementNotificationOutboxEmailDead(dead);
            }

            log.info(
                    "event=notification_outbox_email_processor_completed requestId={} processed={} sent={} failed={} dead={} durationMs={}",
                    requestId,
                    processed,
                    sent,
                    failed,
                    dead,
                    elapsedMs(startedAt)
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=notification_outbox_email_processor_failed requestId={} durationMs={} errorType={} error={}",
                    requestId,
                    elapsedMs(startedAt),
                    ex.getClass().getSimpleName(),
                    OperationalLogSanitizer.safe(ex.getMessage()),
                    ex
            );
            throw ex;
        }
    }

    private ProcessingOutcome processSingleMessage(NotificationOutboxMessage message, Instant now, String requestId) {
        message.markProcessing(now);
        notificationOutboxRepository.save(message);

        EmailOutboxPayload payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), EmailOutboxPayload.class);
            validatePayload(payload);
        } catch (RuntimeException | JsonProcessingException ex) {
            message.markDead(now, "invalid_payload");
            notificationOutboxRepository.save(message);
            logDead(message.getId(), "invalid_payload", requestId);
            return ProcessingOutcome.DEAD;
        }

        EmailProviderClient providerClient = providerClients.get(normalize(payload.provider()));
        if (providerClient == null || isBlank(emailNotificationProperties.getFrom())) {
            return failOrDead(message, now, requestId, "provider_unavailable");
        }

        try {
            providerClient.send(new EmailNotificationRequest(
                    emailNotificationProperties.getFrom().trim(),
                    List.of(payload.to().trim()),
                    payload.subject().trim(),
                    payload.textBody().trim(),
                    payload.htmlBody()
            ));
            message.markSent(now);
            notificationOutboxRepository.save(message);
            return ProcessingOutcome.SENT;
        } catch (RuntimeException ex) {
            String reason = normalizeFailureReason(ex);
            return failOrDead(message, now, requestId, reason);
        }
    }

    private ProcessingOutcome failOrDead(NotificationOutboxMessage message, Instant now, String requestId, String reason) {
        if (message.getAttempts() + 1 >= message.getMaxAttempts()) {
            message.markDead(now, reason);
            notificationOutboxRepository.save(message);
            logDead(message.getId(), reason, requestId);
            return ProcessingOutcome.DEAD;
        }

        message.markFailed(now, now.plus(effectiveRetryBackoff()), reason);
        notificationOutboxRepository.save(message);
        logFailed(message.getId(), reason, requestId);
        return ProcessingOutcome.FAILED;
    }

    private void validatePayload(EmailOutboxPayload payload) {
        if (payload == null
                || isBlank(payload.provider())
                || isBlank(payload.to())
                || isBlank(payload.subject())
                || isBlank(payload.textBody())) {
            throw new IllegalStateException("invalid_payload");
        }
    }

    private void logFailed(Long messageId, String reason, String requestId) {
        log.warn(
                "event=notification_outbox_email_send_failed requestId={} messageId={} reason={}",
                requestId,
                messageId,
                OperationalLogSanitizer.safe(reason)
        );
    }

    private void logDead(Long messageId, String reason, String requestId) {
        log.warn(
                "event=notification_outbox_email_dead requestId={} messageId={} reason={}",
                requestId,
                messageId,
                OperationalLogSanitizer.safe(reason)
        );
    }

    private int effectiveBatchSize() {
        return properties.getProcessorBatchSize() > 0 ? properties.getProcessorBatchSize() : 20;
    }

    private Duration effectiveRetryBackoff() {
        Duration configured = properties.getRetryBackoff();
        if (configured == null || configured.isZero() || configured.isNegative()) {
            return Duration.ofMinutes(1);
        }
        return configured;
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String normalizeFailureReason(RuntimeException ex) {
        return isBlank(ex.getMessage()) ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private enum ProcessingOutcome {
        SENT,
        FAILED,
        DEAD
    }

    private record EmailOutboxPayload(
            String provider,
            String to,
            String subject,
            String textBody,
            String htmlBody
    ) {
    }
}
