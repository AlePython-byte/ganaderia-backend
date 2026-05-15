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
public class OutboxAdminAbuseProtectionService {

    private static final Logger log = LoggerFactory.getLogger(OutboxAdminAbuseProtectionService.class);

    private static final String SCOPE_OUTBOX_REQUEUE_USER = "OUTBOX_REQUEUE_USER";
    private static final String SCOPE_OUTBOX_REQUEUE_IP = "OUTBOX_REQUEUE_IP";
    private static final String SCOPE_OUTBOX_REQUEUE_USER_MESSAGE = "OUTBOX_REQUEUE_USER_MESSAGE";
    private static final String SCOPE_OUTBOX_REQUEUE_USER_IP = "OUTBOX_REQUEUE_USER_IP";
    private static final String GENERIC_LIMIT_MESSAGE = "Demasiadas solicitudes. Intenta nuevamente mas tarde";
    private static final int MAX_KEY_SOURCE_LENGTH = 256;

    private final AbuseProtectionProperties properties;
    private final AbuseProtectionService abuseProtectionService;

    public OutboxAdminAbuseProtectionService(AbuseProtectionProperties properties,
                                             AbuseProtectionService abuseProtectionService) {
        this.properties = properties;
        this.abuseProtectionService = abuseProtectionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = TooManyRequestsException.class)
    public void recordRequeueRequest(String authenticatedUser, String clientIp, Long messageId) {
        if (!isEnabled()) {
            return;
        }

        OutboxRequeueKeys keys = OutboxRequeueKeys.from(authenticatedUser, clientIp, messageId);
        AbuseProtectionPolicy policy = policy();
        long retryAfterSeconds = 0;

        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_OUTBOX_REQUEUE_USER, keys.userKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_OUTBOX_REQUEUE_IP, keys.ipKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_OUTBOX_REQUEUE_USER_MESSAGE, keys.userMessageKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_OUTBOX_REQUEUE_USER_IP, keys.userIpKey(), policy)
        );

        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException(GENERIC_LIMIT_MESSAGE, retryAfterSeconds);
        }
    }

    private long retryAfterSeconds(String scope, String key, AbuseProtectionPolicy policy) {
        AbuseProtectionDecision decision = abuseProtectionService.recordAttempt(scope, key, policy);
        if (!decision.allowed()) {
            log.warn(
                    "event=abuse_protection_limited scope={} keyHash={} retryAfterSeconds={} status=429",
                    scope,
                    key.substring(0, Math.min(12, key.length())),
                    decision.retryAfterSeconds()
            );
            return decision.retryAfterSeconds();
        }

        return 0;
    }

    private boolean isEnabled() {
        return properties.isEnabled()
                && properties.getOutboxRequeue() != null
                && properties.getOutboxRequeue().isEnabled();
    }

    private AbuseProtectionPolicy policy() {
        AbuseProtectionProperties.OutboxRequeue outboxRequeue = properties.getOutboxRequeue();
        return new AbuseProtectionPolicy(
                outboxRequeue.getWindow(),
                outboxRequeue.getMaxAttempts(),
                outboxRequeue.getBlockDuration()
        );
    }

    private record OutboxRequeueKeys(String userKey, String ipKey, String userMessageKey, String userIpKey) {

        static OutboxRequeueKeys from(String authenticatedUser, String clientIp, Long messageId) {
            String normalizedUser = normalizeRequired(authenticatedUser).toLowerCase(Locale.ROOT);
            String normalizedIp = normalizeRequired(clientIp);
            String normalizedMessageId = messageId == null ? "UNKNOWN" : String.valueOf(messageId);

            return new OutboxRequeueKeys(
                    hash(normalizedUser),
                    hash(normalizedIp),
                    hash(normalizedUser + "|" + normalizedMessageId),
                    hash(normalizedUser + "|" + normalizedIp)
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
