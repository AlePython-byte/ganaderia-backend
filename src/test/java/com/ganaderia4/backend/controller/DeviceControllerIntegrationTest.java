package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String DEVICE_LOCATION_PATH = "/api/device/locations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private LocationRepository locationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        collarRepository.deleteAll();
        cowRepository.deleteAll();
    }

    @Test
    void shouldRegisterLocationFromDeviceWhenHeadersSignatureAndPayloadAreValid() throws Exception {
        Cow cow = createCow("VACA-DEVICE-001", "Luna");
        Collar collar = createCollar("COLLAR-DEVICE-001", cow, CollarStatus.ACTIVO, DeviceSignalStatus.SIN_SENAL);

        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        String body = """
                {
                  "latitude": 1.214,
                  "longitude": -77.281,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(signedDeviceLocationRequest(collar.getToken(), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cowToken").value("VACA-DEVICE-001"))
                .andExpect(jsonPath("$.cowName").value("Luna"))
                .andExpect(jsonPath("$.collarToken").value("COLLAR-DEVICE-001"))
                .andExpect(jsonPath("$.latitude").value(1.214))
                .andExpect(jsonPath("$.longitude").value(-77.281));

        assertEquals(1, locationRepository.count());

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(CollarStatus.ACTIVO, updatedCollar.getStatus());
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertNotNull(updatedCollar.getLastSeenAt());
    }

    @Test
    void shouldRejectRequestWhenDeviceTokenHeaderIsMissing() throws Exception {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        String body = """
                {
                  "latitude": 1.100,
                  "longitude": -77.100,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(post(DEVICE_LOCATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token de dispositivo ausente o invalido"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));
    }

    @Test
    void shouldRejectRequestWhenDeviceSignatureIsMissing() throws Exception {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        String body = """
                {
                  "latitude": 1.120,
                  "longitude": -77.120,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(post(DEVICE_LOCATION_PATH)
                        .header("X-Device-Token", "COLLAR-DEVICE-001")
                        .header("X-Device-Timestamp", Instant.now().toString())
                        .header("X-Device-Nonce", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Firma de dispositivo ausente o invalida"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));
    }

    @Test
    void shouldRejectRequestWhenDeviceTokenDoesNotExist() throws Exception {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        String body = """
                {
                  "latitude": 1.150,
                  "longitude": -77.150,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(signedDeviceLocationRequest("COLLAR-INEXISTENTE", body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Collar no registrado"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));
    }

    @Test
    void shouldRejectFutureTimestampBeyondTolerance() throws Exception {
        Cow cow = createCow("VACA-DEVICE-002", "Canela");
        Collar collar = createCollar("COLLAR-DEVICE-002", cow, CollarStatus.ACTIVO, DeviceSignalStatus.MEDIA);

        LocalDateTime timestamp = LocalDateTime.now().plusMinutes(10);

        String body = """
                {
                  "latitude": 1.300,
                  "longitude": -77.300,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(signedDeviceLocationRequest(collar.getToken(), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El timestamp reportado no puede estar demasiado en el futuro"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));
    }

    @Test
    void shouldRejectRequestWhenSignatureTimestampIsExpired() throws Exception {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        String body = """
                {
                  "latitude": 1.330,
                  "longitude": -77.330,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(signedDeviceLocationRequest(
                        "COLLAR-DEVICE-EXPIRED",
                        body,
                        Instant.now().minusSeconds(301),
                        UUID.randomUUID().toString()
                ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Timestamp de dispositivo expirado o fuera de ventana"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));
    }

    @Test
    void shouldRejectRequestWhenSignatureDoesNotMatchPayload() throws Exception {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

        String body = """
                {
                  "latitude": 1.360,
                  "longitude": -77.360,
                  "timestamp": "%s"
                }
                """.formatted(timestamp.withNano(0));

        mockMvc.perform(post(DEVICE_LOCATION_PATH)
                        .header("X-Device-Token", "COLLAR-DEVICE-BAD-SIGNATURE")
                        .header("X-Device-Timestamp", Instant.now().toString())
                        .header("X-Device-Nonce", UUID.randomUUID().toString())
                        .header("X-Device-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Firma de dispositivo invalida"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));
    }

    @Test
    void shouldRejectReplayWhenNonceIsReused() throws Exception {
        Cow cow = createCow("VACA-DEVICE-REPLAY", "Replay");
        Collar collar = createCollar("COLLAR-DEVICE-REPLAY", cow, CollarStatus.ACTIVO, DeviceSignalStatus.MEDIA);

        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1).withNano(0);
        String body = """
                {
                  "latitude": 1.410,
                  "longitude": -77.410,
                  "timestamp": "%s"
                }
                """.formatted(timestamp);

        Instant signatureTimestamp = Instant.now();
        String nonce = UUID.randomUUID().toString();

        mockMvc.perform(signedDeviceLocationRequest(collar.getToken(), body, signatureTimestamp, nonce))
                .andExpect(status().isCreated());

        mockMvc.perform(signedDeviceLocationRequest(collar.getToken(), body, signatureTimestamp, nonce))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEVICE_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Nonce de dispositivo ya utilizado"))
                .andExpect(jsonPath("$.path").value(DEVICE_LOCATION_PATH));

        assertEquals(1, locationRepository.count());
    }

    @Test
    void shouldIgnoreDuplicateDeviceLocationSubmissionWhenNonceIsDifferent() throws Exception {
        Cow cow = createCow("VACA-DEVICE-003", "Estrella");
        Collar collar = createCollar("COLLAR-DEVICE-003", cow, CollarStatus.ACTIVO, DeviceSignalStatus.MEDIA);

        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(2).withNano(0);

        String body = """
                {
                  "latitude": 1.450,
                  "longitude": -77.450,
                  "timestamp": "%s"
                }
                """.formatted(timestamp);

        MvcResult firstResult = mockMvc.perform(signedDeviceLocationRequest(collar.getToken(), body))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(signedDeviceLocationRequest(collar.getToken(), body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        assertEquals(firstJson.get("id").asLong(), secondJson.get("id").asLong());
        assertEquals(1, locationRepository.count());

        Optional<Location> saved = locationRepository.findById(firstJson.get("id").asLong());
        assertTrue(saved.isPresent());
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

    private Cow createCow(String token, String name) {
        Cow cow = new Cow();
        cow.setToken(token);
        cow.setInternalCode("INT-" + token);
        cow.setName(name);
        cow.setStatus(CowStatus.SIN_UBICACION);
        return cowRepository.save(cow);
    }

    private Collar createCollar(String token,
                                Cow cow,
                                CollarStatus status,
                                DeviceSignalStatus signalStatus) {
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setCow(cow);
        collar.setStatus(status);
        collar.setEnabled(true);
        collar.setBatteryLevel(85);
        collar.setSignalStatus(signalStatus);
        collar.setLastSeenAt(LocalDateTime.now().minusHours(2));
        return collarRepository.save(collar);
    }
}
