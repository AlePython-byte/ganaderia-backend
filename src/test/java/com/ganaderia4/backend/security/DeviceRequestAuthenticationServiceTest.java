package com.ganaderia4.backend.security;

import com.ganaderia4.backend.exception.DeviceUnauthorizedException;
import com.ganaderia4.backend.observability.DomainMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.slf4j.MDC;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class DeviceRequestAuthenticationServiceTest {

    private static final String METHOD = "POST";
    private static final String PATH = "/api/device/locations";
    private static final String BODY = """
            {
              "latitude": 1.214,
              "longitude": -77.281,
              "timestamp": "2026-04-18T10:00:00"
            }
            """;

    @Test
    void shouldAuthenticateSignedRequest() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String token = "COLLAR-UNIT-001";
        String secret = "SECRET-UNIT-001";
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry),
                new InMemoryDeviceReplayProtectionStore(),
                new FixedDeviceSigningSecretService(Map.of(token, secret))
        );
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign(secret, timestamp, nonce, BODY);

        String authenticatedToken = service.authenticate(token, timestamp, nonce, signature, METHOD, PATH, BODY);

        assertEquals(token, authenticatedToken);
        assertEquals(1.0, meterRegistry.counter("ganaderia.device.requests.accepted").count());
    }

    @Test
    void shouldRejectInvalidSignature(CapturedOutput output) {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String signature = "invalid-signature";
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry),
                new InMemoryDeviceReplayProtectionStore(),
                new FixedDeviceSigningSecretService(Map.of("COLLAR-UNIT-002", "SECRET-UNIT-002"))
        );
        MDC.put("requestId", "req-device-invalid-signature-001");

        assertThrows(DeviceUnauthorizedException.class, () -> service.authenticate(
                "COLLAR-UNIT-002",
                Instant.now().toString(),
                UUID.randomUUID().toString(),
                signature,
                METHOD,
                PATH,
                BODY
        ));

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.device.requests.rejected",
                "reason",
                "invalid_signature"
        ).count());

        String logs = output.getOut();
        assertTrue(logs.contains("event=device_signature_invalid"));
        assertTrue(logs.contains("reason=invalid_signature"));
        assertTrue(logs.contains("requestId=req-device-invalid-signature-001"));
        assertTrue(logs.contains("method=POST"));
        assertTrue(logs.contains("path=" + PATH));
        assertTrue(logs.contains("device=****-002"));
        assertTrue(logs.contains("status=401"));
        assertFalse(logs.contains("device=COLLAR-UNIT-002"));
        MDC.clear();
    }

    @Test
    void shouldRejectExpiredTimestamp() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String token = "COLLAR-UNIT-003";
        String secret = "SECRET-UNIT-003";
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry),
                new InMemoryDeviceReplayProtectionStore(),
                new FixedDeviceSigningSecretService(Map.of(token, secret))
        );
        String timestamp = Instant.now().minusSeconds(301).toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign(secret, timestamp, nonce, BODY);

        assertThrows(DeviceUnauthorizedException.class, () -> service.authenticate(
                token,
                timestamp,
                nonce,
                signature,
                METHOD,
                PATH,
                BODY
        ));

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.device.requests.rejected",
                "reason",
                "expired_timestamp"
        ).count());
    }

    @Test
    void shouldRejectReusedNonce() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String token = "COLLAR-UNIT-004";
        String secret = "SECRET-UNIT-004";
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry),
                new InMemoryDeviceReplayProtectionStore(),
                new FixedDeviceSigningSecretService(Map.of(token, secret))
        );
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign(secret, timestamp, nonce, BODY);

        service.authenticate(token, timestamp, nonce, signature, METHOD, PATH, BODY);

        assertThrows(DeviceUnauthorizedException.class, () -> service.authenticate(
                token,
                timestamp,
                nonce,
                signature,
                METHOD,
                PATH,
                BODY
        ));

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.device.requests.rejected",
                "reason",
                "replayed_nonce"
        ).count());
    }

    @Test
    void shouldRejectUnknownDeviceBeforeRegisteringNonce() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry),
                new InMemoryDeviceReplayProtectionStore(),
                new FixedDeviceSigningSecretService(Map.of())
        );
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign("UNKNOWN-SECRET", timestamp, nonce, BODY);

        assertThrows(DeviceUnauthorizedException.class, () -> service.authenticate(
                "COLLAR-UNKNOWN",
                timestamp,
                nonce,
                signature,
                METHOD,
                PATH,
                BODY
        ));

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.device.requests.rejected",
                "reason",
                "unknown_device"
        ).count());
    }

    private String sign(String secret, String timestamp, String nonce, String body) throws Exception {
        String canonicalRequest = METHOD
                + "\n" + PATH
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + body;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
    }

    private static class InMemoryDeviceReplayProtectionStore implements DeviceReplayProtectionStore {

        private final Map<String, Instant> usedNonces = new ConcurrentHashMap<>();

        @Override
        public boolean registerNonce(String deviceToken, String nonce, Instant expiresAt) {
            Instant now = Instant.now();
            usedNonces.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));

            String nonceKey = deviceToken + ":" + nonce;
            return usedNonces.putIfAbsent(nonceKey, expiresAt) == null;
        }
    }

    private static class FixedDeviceSigningSecretService implements DeviceSigningSecretService {

        private final Map<String, String> secrets;

        private FixedDeviceSigningSecretService(Map<String, String> secrets) {
            this.secrets = new HashMap<>(secrets);
        }

        @Override
        public Optional<String> resolveSigningSecret(String deviceToken) {
            return Optional.ofNullable(secrets.get(deviceToken));
        }
    }
}
