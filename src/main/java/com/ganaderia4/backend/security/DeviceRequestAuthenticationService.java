package com.ganaderia4.backend.security;

import com.ganaderia4.backend.exception.DeviceUnauthorizedException;
import com.ganaderia4.backend.observability.DomainMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

@Service
public class DeviceRequestAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceRequestAuthenticationService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_DEVICE_TOKEN_LENGTH = 100;
    private static final int MAX_NONCE_LENGTH = 128;
    private static final int MAX_SIGNATURE_LENGTH = 256;

    private final Duration validityWindow;
    private final String hmacPepper;
    private final DomainMetricsService domainMetricsService;
    private final DeviceReplayProtectionStore replayProtectionStore;
    private final DeviceSigningSecretService deviceSigningSecretService;

    public DeviceRequestAuthenticationService(
            @Value("${device.auth.window-seconds:300}") long windowSeconds,
            @Value("${device.auth.hmac-pepper:}") String hmacPepper,
            DomainMetricsService domainMetricsService,
            DeviceReplayProtectionStore replayProtectionStore,
            DeviceSigningSecretService deviceSigningSecretService
    ) {
        this.validityWindow = Duration.ofSeconds(windowSeconds);
        this.hmacPepper = hmacPepper == null ? "" : hmacPepper;
        this.domainMetricsService = domainMetricsService;
        this.replayProtectionStore = replayProtectionStore;
        this.deviceSigningSecretService = deviceSigningSecretService;
    }

    public String authenticate(String deviceToken,
                               String timestampHeader,
                               String nonce,
                               String signature,
                               String method,
                               String path,
                               String rawBody) {
        String sanitizedToken = sanitizeDeviceToken(deviceToken, path);
        Instant requestTimestamp = parseAndValidateTimestamp(timestampHeader, sanitizedToken, path);
        String sanitizedNonce = sanitizeNonce(nonce, sanitizedToken, path);
        String sanitizedSignature = sanitizeSignature(signature, sanitizedToken, path);

        String canonicalRequest = buildCanonicalRequest(
                method,
                path,
                timestampHeader.trim(),
                sanitizedNonce,
                rawBody
        );

        String expectedSignature = calculateSignature(sanitizedToken, canonicalRequest, path);
        if (!constantTimeEquals(expectedSignature, sanitizedSignature)) {
            throw unauthorized("invalid_signature", "Firma de dispositivo invalida", sanitizedToken, path);
        }

        registerNonce(sanitizedToken, sanitizedNonce, requestTimestamp, path);
        domainMetricsService.incrementDeviceRequestAccepted();
        return sanitizedToken;
    }

    private String sanitizeDeviceToken(String deviceToken, String path) {
        if (deviceToken == null || deviceToken.isBlank()) {
            throw unauthorized("missing_token", "Token de dispositivo ausente o invalido", deviceToken, path);
        }

        String sanitizedToken = deviceToken.trim();
        if (sanitizedToken.length() > MAX_DEVICE_TOKEN_LENGTH) {
            throw unauthorized("token_too_long", "Token de dispositivo demasiado largo o invalido", sanitizedToken, path);
        }

        return sanitizedToken;
    }

    private String sanitizeNonce(String nonce, String deviceToken, String path) {
        if (nonce == null || nonce.isBlank()) {
            throw unauthorized("missing_nonce", "Nonce de dispositivo ausente o invalido", deviceToken, path);
        }

        String sanitizedNonce = nonce.trim();
        if (sanitizedNonce.length() > MAX_NONCE_LENGTH) {
            throw unauthorized("nonce_too_long", "Nonce de dispositivo demasiado largo o invalido", deviceToken, path);
        }

        return sanitizedNonce;
    }

    private String sanitizeSignature(String signature, String deviceToken, String path) {
        if (signature == null || signature.isBlank()) {
            throw unauthorized("missing_signature", "Firma de dispositivo ausente o invalida", deviceToken, path);
        }

        String sanitizedSignature = signature.trim();
        if (sanitizedSignature.length() > MAX_SIGNATURE_LENGTH) {
            throw unauthorized("signature_too_long", "Firma de dispositivo demasiado larga o invalida", deviceToken, path);
        }

        return sanitizedSignature;
    }

    private Instant parseAndValidateTimestamp(String timestampHeader, String deviceToken, String path) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw unauthorized("missing_timestamp", "Timestamp de dispositivo ausente o invalido", deviceToken, path);
        }

        Instant requestTimestamp;
        try {
            requestTimestamp = Instant.parse(timestampHeader.trim());
        } catch (DateTimeParseException ex) {
            throw unauthorized("invalid_timestamp", "Timestamp de dispositivo ausente o invalido", deviceToken, path);
        }

        Instant now = Instant.now();
        if (requestTimestamp.isBefore(now.minus(validityWindow))
                || requestTimestamp.isAfter(now.plus(validityWindow))) {
            throw unauthorized("expired_timestamp", "Timestamp de dispositivo expirado o fuera de ventana", deviceToken, path);
        }

        return requestTimestamp;
    }

    private String buildCanonicalRequest(String method,
                                         String path,
                                         String timestamp,
                                         String nonce,
                                         String rawBody) {
        return safe(method).toUpperCase()
                + "\n" + safe(path)
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + safe(rawBody);
    }

    private String calculateSignature(String deviceToken, String canonicalRequest, String path) {
        byte[] signingSecret = resolveSigningSecret(deviceToken, path);

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw unauthorized("signature_validation_error", "No fue posible validar la firma del dispositivo", deviceToken, path);
        }
    }

    private byte[] resolveSigningSecret(String deviceToken, String path) {
        String deviceSecret = deviceSigningSecretService.resolveSigningSecret(deviceToken)
                .filter(secret -> !secret.isBlank())
                .orElseThrow(() -> unauthorized("unknown_device", "Dispositivo no autorizado", deviceToken, path));

        String secret = hmacPepper.isBlank() ? deviceSecret : deviceSecret + ":" + hmacPepper;
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String expectedSignature, String providedSignature) {
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void registerNonce(String deviceToken, String nonce, Instant requestTimestamp, String path) {
        Instant expiresAt = requestTimestamp.plus(validityWindow);

        if (!replayProtectionStore.registerNonce(deviceToken, nonce, expiresAt)) {
            throw unauthorized("replayed_nonce", "Nonce de dispositivo ya utilizado", deviceToken, path);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private DeviceUnauthorizedException unauthorized(String reason, String message, String deviceToken, String path) {
        domainMetricsService.incrementDeviceRequestRejected(reason);
        log.warn(
                "event=security_auth_failed reason={} path={} device={} status={}",
                reason,
                safeLogValue(path),
                maskDeviceToken(deviceToken),
                401
        );
        return new DeviceUnauthorizedException(message);
    }

    private String maskDeviceToken(String token) {
        if (token == null || token.isBlank()) {
            return "UNKNOWN";
        }

        String sanitized = safeLogValue(token.trim());
        if (sanitized.length() <= 4) {
            return "****";
        }

        return "****" + sanitized.substring(sanitized.length() - 4);
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replaceAll("[\\r\\n\\t ]+", "_");
    }
}
