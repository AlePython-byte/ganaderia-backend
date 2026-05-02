package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public EmailNotificationService(EmailNotificationProperties properties,
                                    List<EmailProviderClient> providerClients,
                                    AlertEmailTemplateBuilder templateBuilder) {
        this.properties = properties;
        this.providerClients = providerClients.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        client -> normalize(client.getProviderName()),
                        client -> client
                ));
        this.templateBuilder = templateBuilder;
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

        EmailNotificationContent content = templateBuilder.build(notificationMessage);
        EmailNotificationRequest request = new EmailNotificationRequest(
                properties.getFrom().trim(),
                properties.getTo().trim(),
                content.subject(),
                content.textBody(),
                content.htmlBody()
        );

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

        if (isBlank(properties.getTo())) {
            return "missing_to";
        }

        if (isBlank(properties.getProvider())) {
            return "missing_provider";
        }

        return null;
    }
    private void logSkipped(String reason) {
        logger.info(
                "event=email_notification_skipped requestId={} reason={} provider={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(reason),
                OperationalLogSanitizer.safe(properties.getProvider())
        );
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
