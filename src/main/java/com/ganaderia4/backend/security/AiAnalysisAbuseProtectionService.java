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
public class AiAnalysisAbuseProtectionService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisAbuseProtectionService.class);

    private static final String SCOPE_AI_SUMMARY_USER = "AI_SUMMARY_USER";
    private static final String SCOPE_AI_SUMMARY_IP = "AI_SUMMARY_IP";
    private static final String SCOPE_AI_SUMMARY_USER_IP = "AI_SUMMARY_USER_IP";
    private static final String GENERIC_LIMIT_MESSAGE = "Demasiadas solicitudes. Intenta nuevamente mas tarde";
    private static final int MAX_KEY_SOURCE_LENGTH = 256;

    private final AbuseProtectionProperties properties;
    private final AbuseProtectionService abuseProtectionService;

    public AiAnalysisAbuseProtectionService(AbuseProtectionProperties properties,
                                            AbuseProtectionService abuseProtectionService) {
        this.properties = properties;
        this.abuseProtectionService = abuseProtectionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = TooManyRequestsException.class)
    public void recordAiSummaryRequest(String authenticatedUser, String clientIp) {
        if (!isEnabled()) {
            return;
        }

        AiSummaryKeys keys = AiSummaryKeys.from(authenticatedUser, clientIp);
        AbuseProtectionPolicy policy = policy();
        long retryAfterSeconds = 0;

        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_AI_SUMMARY_USER, keys.userKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_AI_SUMMARY_IP, keys.ipKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_AI_SUMMARY_USER_IP, keys.userIpKey(), policy)
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
                && properties.getAiSummary() != null
                && properties.getAiSummary().isEnabled();
    }

    private AbuseProtectionPolicy policy() {
        AbuseProtectionProperties.AiSummary aiSummary = properties.getAiSummary();
        return new AbuseProtectionPolicy(
                aiSummary.getWindow(),
                aiSummary.getMaxAttempts(),
                aiSummary.getBlockDuration()
        );
    }

    private record AiSummaryKeys(String userKey, String ipKey, String userIpKey) {

        static AiSummaryKeys from(String authenticatedUser, String clientIp) {
            String normalizedUser = normalizeRequired(authenticatedUser).toLowerCase(Locale.ROOT);
            String normalizedIp = normalizeRequired(clientIp);

            return new AiSummaryKeys(
                    hash(normalizedUser),
                    hash(normalizedIp),
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
