package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionProperties;
import com.ganaderia4.backend.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class LoginAbuseProtectionService {

    private static final Logger log = LoggerFactory.getLogger(LoginAbuseProtectionService.class);

    private static final String SCOPE_LOGIN_IP = "LOGIN_IP";
    private static final String SCOPE_LOGIN_EMAIL = "LOGIN_EMAIL";
    private static final String SCOPE_LOGIN_IP_EMAIL = "LOGIN_IP_EMAIL";
    private static final String GENERIC_LIMIT_MESSAGE = "Demasiadas solicitudes. Intenta nuevamente mas tarde";

    private final AbuseProtectionProperties properties;
    private final AbuseProtectionService abuseProtectionService;

    public LoginAbuseProtectionService(AbuseProtectionProperties properties,
                                       AbuseProtectionService abuseProtectionService) {
        this.properties = properties;
        this.abuseProtectionService = abuseProtectionService;
    }

    public void assertLoginAllowed(String clientIp, String email) {
        if (!isEnabled()) {
            return;
        }

        LoginKeys keys = LoginKeys.from(clientIp, email);
        AbuseProtectionPolicy policy = policy();

        assertAllowed(SCOPE_LOGIN_IP, keys.ipKey(), policy);
        assertAllowed(SCOPE_LOGIN_EMAIL, keys.emailKey(), policy);
        assertAllowed(SCOPE_LOGIN_IP_EMAIL, keys.ipEmailKey(), policy);
    }

    public void recordLoginFailure(String clientIp, String email) {
        if (!isEnabled()) {
            return;
        }

        LoginKeys keys = LoginKeys.from(clientIp, email);
        AbuseProtectionPolicy policy = policy();
        long retryAfterSeconds = 0;

        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_LOGIN_IP, keys.ipKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_LOGIN_EMAIL, keys.emailKey(), policy)
        );
        retryAfterSeconds = Math.max(
                retryAfterSeconds,
                retryAfterSeconds(SCOPE_LOGIN_IP_EMAIL, keys.ipEmailKey(), policy)
        );

        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException(GENERIC_LIMIT_MESSAGE, retryAfterSeconds);
        }
    }

    public void recordLoginSuccess(String clientIp, String email) {
        if (!isEnabled()) {
            return;
        }

        LoginKeys keys = LoginKeys.from(clientIp, email);
        abuseProtectionService.reset(SCOPE_LOGIN_EMAIL, keys.emailKey());
        abuseProtectionService.reset(SCOPE_LOGIN_IP_EMAIL, keys.ipEmailKey());
    }

    private void assertAllowed(String scope, String key, AbuseProtectionPolicy policy) {
        AbuseProtectionDecision decision = abuseProtectionService.checkAllowed(scope, key, policy);
        if (!decision.allowed()) {
            logLimited(scope, key, decision.retryAfterSeconds());
            throw new TooManyRequestsException(GENERIC_LIMIT_MESSAGE, decision.retryAfterSeconds());
        }
    }

    private long retryAfterSeconds(String scope, String key, AbuseProtectionPolicy policy) {
        AbuseProtectionDecision decision = abuseProtectionService.recordFailure(scope, key, policy);
        if (!decision.allowed()) {
            logLimited(scope, key, decision.retryAfterSeconds());
            return decision.retryAfterSeconds();
        }

        return 0;
    }

    private void logLimited(String scope, String key, long retryAfterSeconds) {
        log.warn(
                "event=abuse_protection_limited scope={} keyHash={} retryAfterSeconds={} status=429",
                scope,
                key.substring(0, Math.min(12, key.length())),
                retryAfterSeconds
        );
    }

    private boolean isEnabled() {
        return properties.isEnabled() && properties.getLogin().isEnabled();
    }

    private AbuseProtectionPolicy policy() {
        AbuseProtectionProperties.Login login = properties.getLogin();
        return new AbuseProtectionPolicy(
                login.getWindow(),
                login.getMaxAttempts(),
                login.getBlockDuration()
        );
    }

    private record LoginKeys(String ipKey, String emailKey, String ipEmailKey) {

        static LoginKeys from(String clientIp, String email) {
            String normalizedIp = normalize(clientIp);
            String normalizedEmail = normalizeEmail(email);

            return new LoginKeys(
                    hash(normalizedIp),
                    hash(normalizedEmail),
                    hash(normalizedIp + "|" + normalizedEmail)
            );
        }

        private static String normalize(String value) {
            if (value == null || value.isBlank()) {
                return "UNKNOWN";
            }

            return value.trim().replaceAll("[\\r\\n\\t ]+", "_");
        }

        private static String normalizeEmail(String value) {
            return normalize(value).toLowerCase(Locale.ROOT);
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
