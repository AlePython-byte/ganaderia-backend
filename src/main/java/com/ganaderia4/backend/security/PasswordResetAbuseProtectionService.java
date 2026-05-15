package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionProperties;
import com.ganaderia4.backend.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetAbuseProtectionService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetAbuseProtectionService.class);

    private static final String SCOPE_FORGOT_IP = "PASSWORD_RESET_FORGOT_IP";
    private static final String SCOPE_FORGOT_EMAIL = "PASSWORD_RESET_FORGOT_EMAIL";
    private static final String SCOPE_FORGOT_IP_EMAIL = "PASSWORD_RESET_FORGOT_IP_EMAIL";
    private static final String SCOPE_RESET_IP = "PASSWORD_RESET_TOKEN_IP";
    private static final String SCOPE_RESET_TOKEN = "PASSWORD_RESET_TOKEN";
    private static final String SCOPE_RESET_IP_TOKEN = "PASSWORD_RESET_TOKEN_IP_TOKEN";
    private static final String GENERIC_LIMIT_MESSAGE = "Demasiadas solicitudes. Intenta nuevamente mas tarde";
    private static final int MAX_KEY_SOURCE_LENGTH = 256;

    private final AbuseProtectionProperties properties;
    private final AbuseProtectionService abuseProtectionService;

    public PasswordResetAbuseProtectionService(AbuseProtectionProperties properties,
                                               AbuseProtectionService abuseProtectionService) {
        this.properties = properties;
        this.abuseProtectionService = abuseProtectionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = TooManyRequestsException.class)
    public void recordForgotPasswordRequest(String clientIp, String email) {
        if (!isForgotEnabled()) {
            return;
        }

        PasswordResetKeys keys = PasswordResetKeys.from(clientIp, email);
        AbuseProtectionPolicy policy = forgotPolicy();
        long retryAfterSeconds = 0;

        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_FORGOT_IP, keys.ipKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_FORGOT_EMAIL, keys.subjectKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_FORGOT_IP_EMAIL, keys.ipSubjectKey(), policy)
        );

        throwIfLimited(retryAfterSeconds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = TooManyRequestsException.class)
    public void recordResetPasswordRequest(String clientIp, String token) {
        if (!isResetEnabled()) {
            return;
        }

        PasswordResetKeys keys = PasswordResetKeys.from(clientIp, token);
        AbuseProtectionPolicy policy = resetPolicy();
        long retryAfterSeconds = 0;

        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_RESET_IP, keys.ipKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_RESET_TOKEN, keys.subjectKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_RESET_IP_TOKEN, keys.ipSubjectKey(), policy)
        );

        throwIfLimited(retryAfterSeconds);
    }

    private long retryAfterSeconds(String scope, String key, AbuseProtectionPolicy policy) {
        AbuseProtectionDecision decision = abuseProtectionService.recordAttempt(scope, key, policy);
        if (!decision.allowed()) {
            logLimited(scope, key, decision.retryAfterSeconds());
            return decision.retryAfterSeconds();
        }

        return 0;
    }

    private void throwIfLimited(long retryAfterSeconds) {
        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException(GENERIC_LIMIT_MESSAGE, retryAfterSeconds);
        }
    }

    private void logLimited(String scope, String key, long retryAfterSeconds) {
        log.warn(
                "event=abuse_protection_limited scope={} keyHash={} retryAfterSeconds={} status=429",
                scope,
                key.substring(0, Math.min(12, key.length())),
                retryAfterSeconds
        );
    }

    private boolean isForgotEnabled() {
        return properties.isEnabled()
                && properties.getPasswordReset() != null
                && properties.getPasswordReset().getForgot() != null
                && properties.getPasswordReset().getForgot().isEnabled();
    }

    private boolean isResetEnabled() {
        return properties.isEnabled()
                && properties.getPasswordReset() != null
                && properties.getPasswordReset().getReset() != null
                && properties.getPasswordReset().getReset().isEnabled();
    }

    private AbuseProtectionPolicy forgotPolicy() {
        AbuseProtectionProperties.Forgot forgot = properties.getPasswordReset().getForgot();
        return new AbuseProtectionPolicy(
                forgot.getWindow(),
                forgot.getMaxAttempts(),
                forgot.getBlockDuration()
        );
    }

    private AbuseProtectionPolicy resetPolicy() {
        AbuseProtectionProperties.Reset reset = properties.getPasswordReset().getReset();
        return new AbuseProtectionPolicy(
                reset.getWindow(),
                reset.getMaxAttempts(),
                reset.getBlockDuration()
        );
    }

    private record PasswordResetKeys(String ipKey, String subjectKey, String ipSubjectKey) {

        static PasswordResetKeys from(String clientIp, String subject) {
            String normalizedIp = normalizeRequired(clientIp);
            String normalizedSubject = normalizeRequired(subject).toLowerCase(Locale.ROOT);

            return new PasswordResetKeys(
                    hash(normalizedIp),
                    hash(normalizedSubject),
                    hash(normalizedIp + "|" + normalizedSubject)
            );
        }

        private static String normalizeRequired(String value) {
            if (value == null || value.isBlank()) {
                return "UNKNOWN";
            }

            String normalized = value.trim().replaceAll("[\\r\\n\\t ]+", "_");
            if (normalized.length() > MAX_KEY_SOURCE_LENGTH) {
                normalized = normalized.substring(0, MAX_KEY_SOURCE_LENGTH);
            }

            return normalized.isBlank() ? "UNKNOWN" : normalized;
        }

        private static String hash(String value) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("SHA-256 algorithm is not available", ex);
            }
        }
    }
}
