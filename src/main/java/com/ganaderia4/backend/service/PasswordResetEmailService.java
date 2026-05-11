package com.ganaderia4.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.config.FrontendProperties;
import com.ganaderia4.backend.config.PasswordResetProperties;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.EmailDeliveryMode;
import com.ganaderia4.backend.notification.EmailNotificationRequest;
import com.ganaderia4.backend.notification.EmailProviderClient;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationOutboxService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PasswordResetEmailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);
    private static final String PASSWORD_RESET_EVENT_TYPE = "PASSWORD_RESET_REQUESTED";

    private final EmailNotificationProperties emailProperties;
    private final FrontendProperties frontendProperties;
    private final PasswordResetProperties passwordResetProperties;
    private final Map<String, EmailProviderClient> providerClients;
    private final PasswordResetEmailTemplateBuilder templateBuilder;
    private final NotificationOutboxService notificationOutboxService;
    private final ObjectMapper objectMapper;

    public PasswordResetEmailService(EmailNotificationProperties emailProperties,
                                     FrontendProperties frontendProperties,
                                     PasswordResetProperties passwordResetProperties,
                                     List<EmailProviderClient> providerClients,
                                     PasswordResetEmailTemplateBuilder templateBuilder,
                                     NotificationOutboxService notificationOutboxService,
                                     ObjectMapper objectMapper) {
        this.emailProperties = emailProperties;
        this.frontendProperties = frontendProperties;
        this.passwordResetProperties = passwordResetProperties;
        this.providerClients = providerClients.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(
                        client -> normalize(client.getProviderName()),
                        client -> client
                ));
        this.templateBuilder = templateBuilder;
        this.notificationOutboxService = notificationOutboxService;
        this.objectMapper = objectMapper;
    }

    public void sendPasswordResetEmail(User user, PasswordResetTokenIssueResult issuedToken) {
        String maskedEmail = OperationalLogSanitizer.maskEmail(user != null ? user.getEmail() : null);
        String skipReason = resolveSkipReason(user);
        if (skipReason != null) {
            logSkipped(skipReason, maskedEmail);
            return;
        }

        String provider = normalize(emailProperties.getProvider());
        EmailProviderClient providerClient = providerClients.get(provider);
        if (providerClient == null) {
            logSkipped("missing_config", maskedEmail);
            return;
        }

        String resetLink = buildResetLink(frontendProperties.getPasswordResetUrl(), issuedToken.rawToken());
        PasswordResetEmailContent content = templateBuilder.build(user, resetLink, passwordResetProperties.getTokenTtl());
        EmailNotificationRequest request = new EmailNotificationRequest(
                emailProperties.getFrom().trim(),
                List.of(user.getEmail().trim()),
                content.subject(),
                content.textBody(),
                content.htmlBody()
        );

        EmailDeliveryMode deliveryMode = resolveDeliveryMode();
        log.info(
                "event=password_reset_email_delivery_mode_selected requestId={} mode={}",
                OperationalLogSanitizer.requestId(),
                deliveryMode.name()
        );

        if (deliveryMode == EmailDeliveryMode.OUTBOX) {
            enqueueOutbox(provider, user, request, maskedEmail);
            return;
        }

        log.info(
                "event=password_reset_email_direct_send requestId={} email={} provider={}",
                OperationalLogSanitizer.requestId(),
                maskedEmail,
                OperationalLogSanitizer.safe(emailProperties.getProvider())
        );

        try {
            providerClient.send(request);
            log.info(
                    "event=password_reset_email_sent requestId={} email={} provider={}",
                    OperationalLogSanitizer.requestId(),
                    maskedEmail,
                    OperationalLogSanitizer.safe(emailProperties.getProvider())
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "event=password_reset_email_failed requestId={} email={} reason=provider_error provider={} errorType={}",
                    OperationalLogSanitizer.requestId(),
                    maskedEmail,
                    OperationalLogSanitizer.safe(emailProperties.getProvider()),
                    ex.getClass().getSimpleName()
            );
        }
    }

    private EmailDeliveryMode resolveDeliveryMode() {
        EmailDeliveryMode resolved = emailProperties.resolveDeliveryMode();
        String rawValue = emailProperties.getDeliveryMode();
        if (!normalize(rawValue).equals(resolved.name().toLowerCase(Locale.ROOT))) {
            log.warn(
                    "event=password_reset_email_delivery_mode_invalid requestId={} configured={} fallback=direct",
                    OperationalLogSanitizer.requestId(),
                    OperationalLogSanitizer.safe(rawValue)
            );
        }
        return resolved;
    }

    private void enqueueOutbox(String provider,
                               User user,
                               EmailNotificationRequest request,
                               String maskedEmail) {
        try {
            notificationOutboxService.enqueue(
                    NotificationChannel.EMAIL,
                    PASSWORD_RESET_EVENT_TYPE,
                    user.getEmail().trim(),
                    request.subject(),
                    buildOutboxPayload(provider, user.getEmail().trim(), request)
            );
            log.info(
                    "event=password_reset_email_enqueued_for_outbox requestId={} email={}",
                    OperationalLogSanitizer.requestId(),
                    maskedEmail
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "event=password_reset_email_failed requestId={} email={} reason=outbox_enqueue_failed errorType={}",
                    OperationalLogSanitizer.requestId(),
                    maskedEmail,
                    ex.getClass().getSimpleName()
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
            throw new IllegalStateException("password_reset_email_outbox_payload_serialization_failed", ex);
        }
    }

    private String resolveSkipReason(User user) {
        if (user == null) {
            return "unknown_user";
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            return "inactive_user";
        }

        if (!emailProperties.isEnabled()) {
            return "email_disabled";
        }

        if (isBlank(emailProperties.getApiKey())
                || isBlank(emailProperties.getFrom())
                || isBlank(emailProperties.getProvider())
                || isBlank(frontendProperties.getPasswordResetUrl())
                || isBlank(user.getEmail())) {
            return "missing_config";
        }

        return null;
    }

    private void logSkipped(String reason, String maskedEmail) {
        log.info(
                "event=password_reset_email_skipped requestId={} reason={} email={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(reason),
                maskedEmail
        );
    }

    private String buildResetLink(String baseUrl, String rawToken) {
        String separator;
        if (baseUrl.contains("?")) {
            separator = (baseUrl.endsWith("?") || baseUrl.endsWith("&")) ? "" : "&";
        } else {
            separator = "?";
        }

        return baseUrl + separator + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
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
