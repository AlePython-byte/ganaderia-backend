package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final EmailNotificationProperties properties;
    private final Map<String, EmailProviderClient> providerClients;
    private final AlertEmailTemplateBuilder templateBuilder;
    private final EmailNotificationRecipientResolver recipientResolver;
    private final NotificationOutboxService notificationOutboxService;
    private final ObjectMapper objectMapper;

    public EmailNotificationService(EmailNotificationProperties properties,
                                    List<EmailProviderClient> providerClients,
                                    AlertEmailTemplateBuilder templateBuilder,
                                    EmailNotificationRecipientResolver recipientResolver,
                                    NotificationOutboxService notificationOutboxService,
                                    ObjectMapper objectMapper) {
        this.properties = properties;
        this.providerClients = providerClients.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        client -> normalize(client.getProviderName()),
                        client -> client
                ));
        this.templateBuilder = templateBuilder;
        this.recipientResolver = recipientResolver;
        this.notificationOutboxService = notificationOutboxService;
        this.objectMapper = objectMapper;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationSendResult send(NotificationMessage notificationMessage) {
        if (notificationMessage == null) {
            return NotificationSendResult.SKIPPED;
        }

        String skipReason = resolveSkipReason();
        if (skipReason != null) {
            logSkipped(skipReason);
            return NotificationSendResult.SKIPPED;
        }

        String provider = normalize(properties.getProvider());
        EmailProviderClient providerClient = providerClients.get(provider);
        if (providerClient == null) {
            logSkipped("unsupported_provider");
            return NotificationSendResult.SKIPPED;
        }

        EmailNotificationRecipientsResolution recipientsResolution = recipientResolver.resolveRecipients(notificationMessage);
        if (recipientsResolution.recipients().isEmpty()) {
            logSkipped("no_recipients", recipientsResolution.severity());
            return NotificationSendResult.SKIPPED;
        }

        logRecipientsResolved(recipientsResolution);
        EmailNotificationContent content = templateBuilder.build(notificationMessage);
        EmailNotificationRequest request = new EmailNotificationRequest(
                properties.getFrom().trim(),
                recipientsResolution.recipients(),
                content.subject(),
                content.textBody(),
                content.htmlBody()
        );

        if (recipientsResolution.globalFallbackUsed()) {
            logGlobalFallbackUsed();
        }

        enqueueOutboxMessages(provider, notificationMessage.getEventType(), request);
        logSendRequested(provider);

        try {
            providerClient.send(request);
            logSent(provider);
            return NotificationSendResult.SENT;
        } catch (RuntimeException ex) {
            logFailed(provider, ex);
            throw ex;
        }
    }

    private String resolveSkipReason() {
        if (!properties.isEnabled()) {
            return "disabled";
        }

        if (isBlank(properties.getApiKey())) {
            return "missing_api_key";
        }

        if (isBlank(properties.getFrom())) {
            return "missing_from";
        }

        if (isBlank(properties.getProvider())) {
            return "missing_provider";
        }

        return null;
    }

    private void logSkipped(String reason) {
        logSkipped(reason, null);
    }

    private void logSkipped(String reason, com.ganaderia4.backend.model.NotificationSeverity severity) {
        logger.info(
                "event=email_notification_skipped requestId={} reason={} provider={} severity={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(reason),
                OperationalLogSanitizer.safe(properties.getProvider()),
                OperationalLogSanitizer.safe(severity != null ? severity.name() : "-")
        );
    }

    private void logRecipientsResolved(EmailNotificationRecipientsResolution resolution) {
        logger.info(
                "event=email_notification_recipients_resolved requestId={} recipients={} severity={}",
                OperationalLogSanitizer.requestId(),
                resolution.recipients().size(),
                OperationalLogSanitizer.safe(resolution.severity().name())
        );
    }

    private void logGlobalFallbackUsed() {
        logger.info(
                "event=email_notification_global_fallback_used requestId={} reason=no_preference_recipients",
                OperationalLogSanitizer.requestId()
        );
    }

    private void enqueueOutboxMessages(String provider, String eventType, EmailNotificationRequest request) {
        int enqueuedCount = 0;
        try {
            for (String recipient : request.to()) {
                notificationOutboxService.enqueue(
                        NotificationChannel.EMAIL,
                        eventType,
                        recipient,
                        request.subject(),
                        buildOutboxPayload(provider, recipient, request)
                );
                enqueuedCount++;
            }
            if (enqueuedCount > 0) {
                logger.info(
                        "event=email_notification_outbox_enqueued requestId={} count={}",
                        OperationalLogSanitizer.requestId(),
                        enqueuedCount
                );
            }
        } catch (RuntimeException ex) {
            logger.warn(
                    "event=email_notification_outbox_enqueue_failed requestId={} reason={}",
                    OperationalLogSanitizer.requestId(),
                    OperationalLogSanitizer.safe(ex.getMessage())
            );
        }
    }

    private String buildOutboxPayload(String provider, String recipient, EmailNotificationRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", provider);
        payload.put("to", recipient);
        payload.put("subject", request.subject());
        payload.put("textBody", request.textBody());
        payload.put("htmlBody", request.htmlBody());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("email_outbox_payload_serialization_failed", ex);
        }
    }

    private void logSendRequested(String provider) {
        logger.info(
                "event=email_notification_send_requested requestId={} provider={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(provider)
        );
    }

    private void logSent(String provider) {
        logger.info(
                "event=email_notification_sent requestId={} provider={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(provider)
        );
    }

    private void logFailed(String provider, RuntimeException ex) {
        logger.warn(
                "event=email_notification_failed requestId={} provider={} reason={} errorType={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(provider),
                OperationalLogSanitizer.safe(ex.getMessage()),
                ex.getClass().getSimpleName()
        );
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
}
