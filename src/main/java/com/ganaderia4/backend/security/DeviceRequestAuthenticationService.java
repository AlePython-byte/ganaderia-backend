package com.ganaderia4.backend.security;

import com.ganaderia4.backend.exception.DeviceUnauthorizedException;
import com.ganaderia4.backend.observability.DomainMetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

@Service
public class DeviceRequestAuthenticationService {

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
        String sanitizedToken = sanitizeDeviceToken(deviceToken);
        Instant requestTimestamp = parseAndValidateTimestamp(timestampHeader);
        String sanitizedNonce = sanitizeNonce(nonce);
        String sanitizedSignature = sanitizeSignature(signature);

        String canonicalRequest = buildCanonicalRequest(
                method,
                path,
                timestampHeader.trim(),
                sanitizedNonce,
                rawBody
        );

        String expectedSignature = calculateSignature(sanitizedToken, canonicalRequest);
        if (!constantTimeEquals(expectedSignature, sanitizedSignature)) {
            throw unauthorized("invalid_signature", "Firma de dispositivo invalida");
        }

        registerNonce(sanitizedToken, sanitizedNonce, requestTimestamp);
        domainMetricsService.incrementDeviceRequestAccepted();
        return sanitizedToken;
    }

    private String sanitizeDeviceToken(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            throw unauthorized("missing_token", "Token de dispositivo ausente o invalido");
        }

        String sanitizedToken = deviceToken.trim();
        if (sanitizedToken.length() > MAX_DEVICE_TOKEN_LENGTH) {
            throw unauthorized("token_too_long", "Token de dispositivo demasiado largo o invalido");
        }

        return sanitizedToken;
    }

    private String sanitizeNonce(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw unauthorized("missing_nonce", "Nonce de dispositivo ausente o invalido");
        }

        String sanitizedNonce = nonce.trim();
        if (sanitizedNonce.length() > MAX_NONCE_LENGTH) {
            throw unauthorized("nonce_too_long", "Nonce de dispositivo demasiado largo o invalido");
        }

        return sanitizedNonce;
    }

    private String sanitizeSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            throw unauthorized("missing_signature", "Firma de dispositivo ausente o invalida");
        }

        String sanitizedSignature = signature.trim();
        if (sanitizedSignature.length() > MAX_SIGNATURE_LENGTH) {
            throw unauthorized("signature_too_long", "Firma de dispositivo demasiado larga o invalida");
        }

        return sanitizedSignature;
    }

    private Instant parseAndValidateTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw unauthorized("missing_timestamp", "Timestamp de dispositivo ausente o invalido");
        }

        Instant requestTimestamp;
        try {
            requestTimestamp = Instant.parse(timestampHeader.trim());
        } catch (DateTimeParseException ex) {
            throw unauthorized("invalid_timestamp", "Timestamp de dispositivo ausente o invalido");
        }

        Instant now = Instant.now();
        if (requestTimestamp.isBefore(now.minus(validityWindow))
                || requestTimestamp.isAfter(now.plus(validityWindow))) {
            throw unauthorized("expired_timestamp", "Timestamp de dispositivo expirado o fuera de ventana");
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

    private String calculateSignature(String deviceToken, String canonicalRequest) {
        byte[] signingSecret = resolveSigningSecret(deviceToken);

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception ex) {
            throw unauthorized("signature_validation_error", "No fue posible validar la firma del dispositivo");
        }
    }

    private byte[] resolveSigningSecret(String deviceToken) {
        String deviceSecret = deviceSigningSecretService.resolveSigningSecret(deviceToken)
                .filter(secret -> !secret.isBlank())
                .orElseThrow(() -> unauthorized("unknown_device", "Dispositivo no autorizado"));

        String secret = hmacPepper.isBlank() ? deviceSecret : deviceSecret + ":" + hmacPepper;
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String expectedSignature, String providedSignature) {
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void registerNonce(String deviceToken, String nonce, Instant requestTimestamp) {
        Instant expiresAt = requestTimestamp.plus(validityWindow);

        if (!replayProtectionStore.registerNonce(deviceToken, nonce, expiresAt)) {
            throw unauthorized("replayed_nonce", "Nonce de dispositivo ya utilizado");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private DeviceUnauthorizedException unauthorized(String reason, String message) {
        domainMetricsService.incrementDeviceRequestRejected(reason);
        return new DeviceUnauthorizedException(message);
    }
}
