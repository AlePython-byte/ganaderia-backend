package com.ganaderia4.backend.security;

import com.ganaderia4.backend.exception.DeviceUnauthorizedException;
import com.ganaderia4.backend.observability.DomainMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry)
        );
        String token = "COLLAR-UNIT-001";
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign(token, timestamp, nonce, BODY);

        String authenticatedToken = service.authenticate(token, timestamp, nonce, signature, METHOD, PATH, BODY);

        assertEquals(token, authenticatedToken);
        assertEquals(1.0, meterRegistry.counter("ganaderia.device.requests.accepted").count());
    }

    @Test
    void shouldRejectInvalidSignature() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry)
        );

        assertThrows(DeviceUnauthorizedException.class, () -> service.authenticate(
                "COLLAR-UNIT-002",
                Instant.now().toString(),
                UUID.randomUUID().toString(),
                "invalid-signature",
                METHOD,
                PATH,
                BODY
        ));

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.device.requests.rejected",
                "reason",
                "invalid_signature"
        ).count());
    }

    @Test
    void shouldRejectExpiredTimestamp() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry)
        );
        String token = "COLLAR-UNIT-003";
        String timestamp = Instant.now().minusSeconds(301).toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign(token, timestamp, nonce, BODY);

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
        DeviceRequestAuthenticationService service = new DeviceRequestAuthenticationService(
                300,
                "",
                new DomainMetricsService(meterRegistry)
        );
        String token = "COLLAR-UNIT-004";
        String timestamp = Instant.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signature = sign(token, timestamp, nonce, BODY);

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

    private String sign(String token, String timestamp, String nonce, String body) throws Exception {
        String canonicalRequest = METHOD
                + "\n" + PATH
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + body;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
    }
}
