package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.GlobalExceptionHandler;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.security.DeviceAbuseProtectionService;
import com.ganaderia4.backend.security.DeviceReplayProtectionStore;
import com.ganaderia4.backend.security.DeviceRequestAuthenticationService;
import com.ganaderia4.backend.security.DeviceSigningSecretService;
import com.ganaderia4.backend.service.LocationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerTest {

    private static final String DEVICE_LOCATION_PATH = "/api/device/locations";
    private static final String DEVICE_TOKEN = "COLLAR-001";
    private static final String DEVICE_SECRET = "SECRET-COLLAR-001";

    private MockMvc mockMvc;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = mock(LocationService.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        DeviceAbuseProtectionService deviceAbuseProtectionService = mock(DeviceAbuseProtectionService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");

        DeviceController controller = new DeviceController(
                locationService,
                new DeviceRequestAuthenticationService(
                        300,
                        "",
                        new DomainMetricsService(new SimpleMeterRegistry()),
                        new InMemoryDeviceReplayProtectionStore(),
                        new FixedDeviceSigningSecretService(Map.of(DEVICE_TOKEN, DEVICE_SECRET))
                ),
                deviceAbuseProtectionService,
                clientIpResolver,
                validator
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldAuthenticateSignedDeviceRequestAndCallLocationService() throws Exception {
        LocationResponseDTO response = new LocationResponseDTO();
        response.setId(10L);
        response.setCowToken("VACA-001");
        response.setCowName("Luna");
        response.setCollarToken("COLLAR-001");
        response.setLatitude(1.214);
        response.setLongitude(-77.281);

        when(locationService.registerLocationFromDevice(any(DeviceLocationPayloadDTO.class)))
                .thenReturn(response);

        String body = """
                {
                  "latitude": 1.214,
                  "longitude": -77.281,
                  "timestamp": "2026-04-18T10:00:00"
                }
                """;

        mockMvc.perform(signedDeviceLocationRequest(DEVICE_TOKEN, DEVICE_SECRET, body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.cowToken").value("VACA-001"))
                .andExpect(jsonPath("$.collarToken").value("COLLAR-001"));

        ArgumentCaptor<DeviceLocationPayloadDTO> captor = ArgumentCaptor.forClass(DeviceLocationPayloadDTO.class);
        verify(locationService).registerLocationFromDevice(captor.capture());

        DeviceLocationPayloadDTO payload = captor.getValue();
        assertEquals(DEVICE_TOKEN, payload.getDeviceToken());
        assertEquals(1.214, payload.getLat());
        assertEquals(-77.281, payload.getLon());
        assertEquals(LocalDateTime.of(2026, 4, 18, 10, 0), payload.getReportedAt());
    }

    @Test
    void shouldRejectUnsignedDeviceRequestBeforeCallingLocationService() throws Exception {
        String body = """
                {
                  "latitude": 1.214,
                  "longitude": -77.281,
                  "timestamp": "2026-04-18T10:00:00"
                }
                """;

        mockMvc.perform(post(DEVICE_LOCATION_PATH)
                        .header("X-Device-Token", "COLLAR-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Timestamp de dispositivo ausente o invalido"));
    }

    @Test
    void shouldRejectReplayedNonce() throws Exception {
        LocationResponseDTO response = new LocationResponseDTO();
        response.setId(10L);

        when(locationService.registerLocationFromDevice(any(DeviceLocationPayloadDTO.class)))
                .thenReturn(response);

        String body = """
                {
                  "latitude": 1.214,
                  "longitude": -77.281,
                  "timestamp": "2026-04-18T10:00:00"
                }
                """;
        Instant timestamp = Instant.now();
        String nonce = UUID.randomUUID().toString();

        mockMvc.perform(signedDeviceLocationRequest(DEVICE_TOKEN, DEVICE_SECRET, body, timestamp, nonce))
                .andExpect(status().isCreated());

        mockMvc.perform(signedDeviceLocationRequest(DEVICE_TOKEN, DEVICE_SECRET, body, timestamp, nonce))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Nonce de dispositivo ya utilizado"));
    }

    private MockHttpServletRequestBuilder signedDeviceLocationRequest(String token, String secret, String body) throws Exception {
        return signedDeviceLocationRequest(token, secret, body, Instant.now(), UUID.randomUUID().toString());
    }

    private MockHttpServletRequestBuilder signedDeviceLocationRequest(String token,
                                                                      String secret,
                                                                      String body,
                                                                      Instant timestamp,
                                                                      String nonce) throws Exception {
        String timestampHeader = timestamp.toString();
        String signature = sign(secret, timestampHeader, nonce, body);

        return post(DEVICE_LOCATION_PATH)
                .header("X-Device-Token", token)
                .header("X-Device-Timestamp", timestampHeader)
                .header("X-Device-Nonce", nonce)
                .header("X-Device-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String sign(String secret, String timestamp, String nonce, String body) throws Exception {
        String canonicalRequest = "POST"
                + "\n" + DEVICE_LOCATION_PATH
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
