package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionProperties;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationMessage;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class EmailNotificationThrottleService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationThrottleService.class);

    private static final String SCOPE_EMAIL_RECIPIENT = "EMAIL_RECIPIENT";
    private static final String SCOPE_EMAIL_RECIPIENT_EVENT_TYPE = "EMAIL_RECIPIENT_EVENT_TYPE";
    private static final String SCOPE_EMAIL_CHANNEL = "EMAIL_CHANNEL";
    private static final int MAX_KEY_SOURCE_LENGTH = 256;

    private final AbuseProtectionProperties properties;
    private final AbuseProtectionService abuseProtectionService;
    private final DomainMetricsService domainMetricsService;

    public EmailNotificationThrottleService(AbuseProtectionProperties properties,
                                            AbuseProtectionService abuseProtectionService,
                                            DomainMetricsService domainMetricsService) {
        this.properties = properties;
        this.abuseProtectionService = abuseProtectionService;
        this.domainMetricsService = domainMetricsService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> filterAllowedRecipients(NotificationMessage notificationMessage, List<String> recipients) {
        if (!isEnabled() || recipients == null || recipients.isEmpty()) {
            return recipients == null ? List.of() : List.copyOf(recipients);
        }

        List<String> allowedRecipients = new ArrayList<>();
        String eventType = normalizeEventType(notificationMessage);
        for (String recipient : recipients) {
            if (isRecipientAllowed(recipient, eventType)) {
                allowedRecipients.add(recipient);
            }
        }

        return allowedRecipients;
    }

    private boolean isRecipientAllowed(String recipient, String eventType) {
        String normalizedRecipient = normalizeRequired(recipient).toLowerCase(Locale.ROOT);
        String recipientKey = hash(normalizedRecipient);
        String recipientEventTypeKey = hash(normalizedRecipient + "|" + eventType);
        String channelKey = hash(NotificationChannel.EMAIL.name());

        if (isLimited(SCOPE_EMAIL_CHANNEL, channelKey, channelPolicy(), eventType)) {
            return false;
        }

        if (isLimited(SCOPE_EMAIL_RECIPIENT, recipientKey, recipientPolicy(), eventType)) {
            return false;
        }

        return !isLimited(SCOPE_EMAIL_RECIPIENT_EVENT_TYPE, recipientEventTypeKey, recipientEventTypePolicy(), eventType);
    }

    private boolean isLimited(String scope, String key, AbuseProtectionPolicy policy, String eventType) {
        AbuseProtectionDecision decision = abuseProtectionService.recordAttempt(scope, key, policy);
        if (decision.allowed()) {
            return false;
        }

        log.warn(
                "event=email_notification_throttled requestId={} channel=EMAIL scope={} keyHash={} notificationType={} throttled=true retryAfterSeconds={}",
                OperationalLogSanitizer.requestId(),
                scope,
                key.substring(0, Math.min(12, key.length())),
                OperationalLogSanitizer.safe(eventType),
                decision.retryAfterSeconds()
        );
        domainMetricsService.incrementEmailNotificationThrottled(eventType, scope);
        return true;
    }

    private boolean isEnabled() {
        return properties.isEnabled()
                && properties.getEmail() != null
                && properties.getEmail().isEnabled();
    }

    private AbuseProtectionPolicy recipientPolicy() {
        return policy(properties.getEmail().getRecipient());
    }

    private AbuseProtectionPolicy recipientEventTypePolicy() {
        return policy(properties.getEmail().getRecipientEventType());
    }

    private AbuseProtectionPolicy channelPolicy() {
        return policy(properties.getEmail().getChannel());
    }

    private AbuseProtectionPolicy policy(AbuseProtectionProperties.EmailLimit limit) {
        AbuseProtectionProperties.EmailLimit safeLimit = limit != null
                ? limit
                : new AbuseProtectionProperties.EmailLimit();
        return new AbuseProtectionPolicy(
                safeLimit.getWindow(),
                safeLimit.getMaxAttempts(),
                safeLimit.getBlockDuration()
        );
    }

    private String normalizeEventType(NotificationMessage notificationMessage) {
        if (notificationMessage == null || notificationMessage.getEventType() == null
                || notificationMessage.getEventType().isBlank()) {
            return "UNKNOWN";
        }

        return normalizeRequired(notificationMessage.getEventType()).toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = value.trim().replaceAll("[\\r\\n\\t ]+", "_");
        if (normalized.length() > MAX_KEY_SOURCE_LENGTH) {
            normalized = normalized.substring(0, MAX_KEY_SOURCE_LENGTH);
        }

        return normalized.isBlank() ? "UNKNOWN" : normalized;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
