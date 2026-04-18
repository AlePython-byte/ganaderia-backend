package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.GlobalExceptionHandler;
import com.ganaderia4.backend.security.DeviceRequestAuthenticationService;
import com.ganaderia4.backend.service.LocationService;
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
import java.util.UUID;

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

    private MockMvc mockMvc;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = mock(LocationService.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        DeviceController controller = new DeviceController(
                locationService,
                new DeviceRequestAuthenticationService(300, ""),
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

        mockMvc.perform(signedDeviceLocationRequest("COLLAR-001", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.cowToken").value("VACA-001"))
                .andExpect(jsonPath("$.collarToken").value("COLLAR-001"));

        ArgumentCaptor<DeviceLocationPayloadDTO> captor = ArgumentCaptor.forClass(DeviceLocationPayloadDTO.class);
        verify(locationService).registerLocationFromDevice(captor.capture());

        DeviceLocationPayloadDTO payload = captor.getValue();
        assertEquals("COLLAR-001", payload.getDeviceToken());
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

        mockMvc.perform(signedDeviceLocationRequest("COLLAR-001", body, timestamp, nonce))
                .andExpect(status().isCreated());

        mockMvc.perform(signedDeviceLocationRequest("COLLAR-001", body, timestamp, nonce))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Nonce de dispositivo ya utilizado"));
    }

    private MockHttpServletRequestBuilder signedDeviceLocationRequest(String token, String body) throws Exception {
        return signedDeviceLocationRequest(token, body, Instant.now(), UUID.randomUUID().toString());
    }

    private MockHttpServletRequestBuilder signedDeviceLocationRequest(String token,
                                                                      String body,
                                                                      Instant timestamp,
                                                                      String nonce) throws Exception {
        String timestampHeader = timestamp.toString();
        String signature = sign(token, timestampHeader, nonce, body);

        return post(DEVICE_LOCATION_PATH)
                .header("X-Device-Token", token)
                .header("X-Device-Timestamp", timestampHeader)
                .header("X-Device-Nonce", nonce)
                .header("X-Device-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String sign(String token, String timestamp, String nonce, String body) throws Exception {
        String canonicalRequest = "POST"
                + "\n" + DEVICE_LOCATION_PATH
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + body;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
    }
}
