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

@Service
public class DeviceAbuseProtectionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAbuseProtectionService.class);

    private static final String SCOPE_DEVICE_IP = "DEVICE_IP";
    private static final String SCOPE_DEVICE_TOKEN = "DEVICE_TOKEN";
    private static final String SCOPE_DEVICE_IP_TOKEN = "DEVICE_IP_TOKEN";
    private static final String GENERIC_LIMIT_MESSAGE = "Demasiadas solicitudes. Intenta nuevamente mas tarde";
    private static final int MAX_KEY_SOURCE_LENGTH = 128;

    private final AbuseProtectionProperties properties;
    private final AbuseProtectionService abuseProtectionService;

    public DeviceAbuseProtectionService(AbuseProtectionProperties properties,
                                        AbuseProtectionService abuseProtectionService) {
        this.properties = properties;
        this.abuseProtectionService = abuseProtectionService;
    }

    public void recordDeviceRequest(String clientIp, String deviceToken, String path) {
        if (!isEnabled()) {
            return;
        }

        DeviceKeys keys = DeviceKeys.from(clientIp, deviceToken);
        AbuseProtectionPolicy policy = policy();
        long retryAfterSeconds = 0;

        if (keys.hasDeviceToken()) {
            retryAfterSeconds = Math.max(
                    retryAfterSeconds,
                    retryAfterSeconds(SCOPE_DEVICE_TOKEN, keys.deviceTokenKey(), policy, path)
            );
            retryAfterSeconds = Math.max(
                    retryAfterSeconds,
                    retryAfterSeconds(SCOPE_DEVICE_IP_TOKEN, keys.ipDeviceTokenKey(), policy, path)
            );
        } else {
            retryAfterSeconds = Math.max(
                    retryAfterSeconds,
                    retryAfterSeconds(SCOPE_DEVICE_IP, keys.ipKey(), policy, path)
            );
        }

        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException(GENERIC_LIMIT_MESSAGE, retryAfterSeconds);
        }
    }

    private long retryAfterSeconds(String scope, String key, AbuseProtectionPolicy policy, String path) {
        AbuseProtectionDecision decision = abuseProtectionService.recordAttempt(scope, key, policy);
        if (!decision.allowed()) {
            log.warn(
                    "event=abuse_protection_limited scope={} path={} keyHash={} retryAfterSeconds={} status=429",
                    scope,
                    safeLogValue(path),
                    key.substring(0, Math.min(12, key.length())),
                    decision.retryAfterSeconds()
            );
            return decision.retryAfterSeconds();
        }

        return 0;
    }

    private boolean isEnabled() {
        return properties.isEnabled() && properties.getDevice().isEnabled();
    }

    private AbuseProtectionPolicy policy() {
        AbuseProtectionProperties.Device device = properties.getDevice();
        return new AbuseProtectionPolicy(
                device.getWindow(),
                device.getMaxAttempts(),
                device.getBlockDuration()
        );
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replaceAll("[\\r\\n\\t ]+", "_");
    }

    private record DeviceKeys(String ipKey, String deviceTokenKey, String ipDeviceTokenKey) {

        static DeviceKeys from(String clientIp, String deviceToken) {
            String normalizedIp = normalizeRequired(clientIp);
            String normalizedDeviceToken = normalizeOptional(deviceToken);

            if (normalizedDeviceToken == null) {
                return new DeviceKeys(hash(normalizedIp), null, null);
            }

            return new DeviceKeys(
                    hash(normalizedIp),
                    hash(normalizedDeviceToken),
                    hash(normalizedIp + "|" + normalizedDeviceToken)
            );
        }

        boolean hasDeviceToken() {
            return deviceTokenKey != null;
        }

        private static String normalizeRequired(String value) {
            String normalized = normalizeOptional(value);
            return normalized == null ? "UNKNOWN" : normalized;
        }

        private static String normalizeOptional(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            String normalized = value.trim().replaceAll("[\\r\\n\\t ]+", "_");
            if (normalized.length() > MAX_KEY_SOURCE_LENGTH) {
                normalized = normalized.substring(0, MAX_KEY_SOURCE_LENGTH);
            }

            return normalized.isBlank() ? null : normalized;
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
