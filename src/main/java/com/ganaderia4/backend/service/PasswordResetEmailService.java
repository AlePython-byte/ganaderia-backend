package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.config.FrontendProperties;
import com.ganaderia4.backend.config.PasswordResetProperties;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.EmailNotificationRequest;
import com.ganaderia4.backend.notification.EmailProviderClient;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PasswordResetEmailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final EmailNotificationProperties emailProperties;
    private final FrontendProperties frontendProperties;
    private final PasswordResetProperties passwordResetProperties;
    private final Map<String, EmailProviderClient> providerClients;
    private final PasswordResetEmailTemplateBuilder templateBuilder;

    public PasswordResetEmailService(EmailNotificationProperties emailProperties,
                                     FrontendProperties frontendProperties,
                                     PasswordResetProperties passwordResetProperties,
                                     List<EmailProviderClient> providerClients,
                                     PasswordResetEmailTemplateBuilder templateBuilder) {
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
    }

    public void sendPasswordResetEmail(User user, PasswordResetTokenIssueResult issuedToken) {
        String maskedEmail = OperationalLogSanitizer.maskEmail(user != null ? user.getEmail() : null);
        String skipReason = resolveSkipReason(user);
        if (skipReason != null) {
            logSkipped(skipReason, maskedEmail);
            return;
        }

        EmailProviderClient providerClient = providerClients.get(normalize(emailProperties.getProvider()));
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

        log.info(
                "event=password_reset_email_send_requested requestId={} email={} provider={}",
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
