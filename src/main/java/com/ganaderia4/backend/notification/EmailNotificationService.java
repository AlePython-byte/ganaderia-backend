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
    private static final String DEFAULT_SUBJECT = "[Ganaderia 4.0] Alerta operativa";

    private final EmailNotificationProperties properties;
    private final Map<String, EmailProviderClient> providerClients;

    public EmailNotificationService(EmailNotificationProperties properties,
                                    List<EmailProviderClient> providerClients) {
        this.properties = properties;
        this.providerClients = providerClients.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        client -> normalize(client.getProviderName()),
                        client -> client
                ));
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

        EmailNotificationRequest request = new EmailNotificationRequest(
                properties.getFrom().trim(),
                properties.getTo().trim(),
                DEFAULT_SUBJECT,
                buildTextBody(notificationMessage)
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

    private String buildTextBody(NotificationMessage notificationMessage) {
        StringBuilder body = new StringBuilder()
                .append("Se genero una alerta operativa en Ganaderia 4.0.").append(System.lineSeparator()).append(System.lineSeparator())
                .append("Evento: ").append(safeLine(notificationMessage.getEventType())).append(System.lineSeparator())
                .append("Severidad: ").append(safeLine(notificationMessage.getSeverity())).append(System.lineSeparator())
                .append("Titulo: ").append(safeLine(notificationMessage.getTitle())).append(System.lineSeparator())
                .append("Mensaje: ").append(safeLine(notificationMessage.getMessage())).append(System.lineSeparator())
                .append("Fecha: ").append(notificationMessage.getCreatedAt()).append(System.lineSeparator());

        if (!notificationMessage.getMetadata().isEmpty()) {
            body.append("Metadata: ");
            body.append(notificationMessage.getMetadata().entrySet().stream()
                    .map(entry -> safeLine(entry.getKey()) + "=" + safeLine(entry.getValue()))
                    .collect(Collectors.joining(", ")));
            body.append(System.lineSeparator());
        }

        return body.toString();
    }

    private String safeLine(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replaceAll("[\\r\\n\\t]+", " ").trim();
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
