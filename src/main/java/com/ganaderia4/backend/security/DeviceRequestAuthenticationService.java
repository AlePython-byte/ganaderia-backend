package com.ganaderia4.backend.security;

import com.ganaderia4.backend.exception.DeviceUnauthorizedException;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceRequestAuthenticationService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_DEVICE_TOKEN_LENGTH = 100;
    private static final int MAX_NONCE_LENGTH = 128;
    private static final int MAX_SIGNATURE_LENGTH = 256;

    private final Duration validityWindow;
    private final String hmacPepper;
    private final Map<String, Instant> usedNonces = new ConcurrentHashMap<>();

    public DeviceRequestAuthenticationService(
            @Value("${device.auth.window-seconds:300}") long windowSeconds,
            @Value("${device.auth.hmac-pepper:}") String hmacPepper
    ) {
        this.validityWindow = Duration.ofSeconds(windowSeconds);
        this.hmacPepper = hmacPepper == null ? "" : hmacPepper;
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
            throw new DeviceUnauthorizedException("Firma de dispositivo invalida");
        }

        registerNonce(sanitizedToken, sanitizedNonce, requestTimestamp);
        return sanitizedToken;
    }

    private String sanitizeDeviceToken(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new DeviceUnauthorizedException("Token de dispositivo ausente o invalido");
        }

        String sanitizedToken = deviceToken.trim();
        if (sanitizedToken.length() > MAX_DEVICE_TOKEN_LENGTH) {
            throw new DeviceUnauthorizedException("Token de dispositivo demasiado largo o invalido");
        }

        return sanitizedToken;
    }

    private String sanitizeNonce(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new DeviceUnauthorizedException("Nonce de dispositivo ausente o invalido");
        }

        String sanitizedNonce = nonce.trim();
        if (sanitizedNonce.length() > MAX_NONCE_LENGTH) {
            throw new DeviceUnauthorizedException("Nonce de dispositivo demasiado largo o invalido");
        }

        return sanitizedNonce;
    }

    private String sanitizeSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            throw new DeviceUnauthorizedException("Firma de dispositivo ausente o invalida");
        }

        String sanitizedSignature = signature.trim();
        if (sanitizedSignature.length() > MAX_SIGNATURE_LENGTH) {
            throw new DeviceUnauthorizedException("Firma de dispositivo demasiado larga o invalida");
        }

        return sanitizedSignature;
    }

    private Instant parseAndValidateTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw new DeviceUnauthorizedException("Timestamp de dispositivo ausente o invalido");
        }

        Instant requestTimestamp;
        try {
            requestTimestamp = Instant.parse(timestampHeader.trim());
        } catch (DateTimeParseException ex) {
            throw new DeviceUnauthorizedException("Timestamp de dispositivo ausente o invalido");
        }

        Instant now = Instant.now();
        if (requestTimestamp.isBefore(now.minus(validityWindow))
                || requestTimestamp.isAfter(now.plus(validityWindow))) {
            throw new DeviceUnauthorizedException("Timestamp de dispositivo expirado o fuera de ventana");
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
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(resolveSigningSecret(deviceToken), HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception ex) {
            throw new DeviceUnauthorizedException("No fue posible validar la firma del dispositivo");
        }
    }

    private byte[] resolveSigningSecret(String deviceToken) {
        String secret = hmacPepper.isBlank() ? deviceToken : deviceToken + ":" + hmacPepper;
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String expectedSignature, String providedSignature) {
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void registerNonce(String deviceToken, String nonce, Instant requestTimestamp) {
        purgeExpiredNonces();

        String nonceKey = deviceToken + ":" + nonce;
        Instant expiresAt = requestTimestamp.plus(validityWindow);
        Instant previous = usedNonces.putIfAbsent(nonceKey, expiresAt);

        if (previous != null && previous.isAfter(Instant.now())) {
            throw new DeviceUnauthorizedException("Nonce de dispositivo ya utilizado");
        }

        if (previous != null) {
            usedNonces.put(nonceKey, expiresAt);
        }
    }

    private void purgeExpiredNonces() {
        Instant now = Instant.now();
        for (Map.Entry<String, Instant> entry : usedNonces.entrySet()) {
            if (!entry.getValue().isAfter(now)) {
                usedNonces.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
