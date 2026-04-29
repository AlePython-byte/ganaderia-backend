package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(DashboardControllerIntegrationTest.FixedClockConfig.class)
class DashboardControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-04-28T18:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        collarRepository.deleteAll();
        cowRepository.deleteAll();
        userRepository.deleteAll();

        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
    }

    @Test
    void shouldExposePrioritizedAlertQueueOnDashboard() throws Exception {
        Cow highPriorityCow = createCow("VACA-020", "Aurora", CowStatus.FUERA);
        Cow lowerPriorityCow = createCow("VACA-021", "Perla", CowStatus.DENTRO);

        createAlert(
                highPriorityCow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE,
                "La vaca sigue fuera de la geocerca",
                FIXED_NOW.minusHours(2)
        );
        createAlert(
                lowerPriorityCow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Collar sin reporte reciente",
                FIXED_NOW.minusMinutes(5)
        );

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/dashboard/prioritized-alert-queue")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("EXIT_GEOFENCE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].priorityScore").value(80))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-020"));
    }

    @Test
    void shouldKeepCriticalAlertsEndpointAsPrioritizedCompatibilityAlias() throws Exception {
        Cow highPriorityCow = createCow("VACA-022", "Sol", CowStatus.FUERA);
        Cow lowerPriorityCow = createCow("VACA-023", "Lluvia", CowStatus.DENTRO);

        createAlert(
                highPriorityCow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE,
                "La vaca sigue fuera de la geocerca",
                FIXED_NOW.minusHours(2)
        );
        createAlert(
                lowerPriorityCow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Collar sin reporte reciente",
                FIXED_NOW.minusMinutes(5)
        );

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/dashboard/critical-alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXIT_GEOFENCE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].priorityScore").value(80));
    }

    @Test
    void shouldExposePendingAlertAgingKpiOnDashboard() throws Exception {
        Cow recentCow = createCow("VACA-030", "Mora", CowStatus.DENTRO);
        Cow mediumCow = createCow("VACA-031", "Bruma", CowStatus.DENTRO);
        Cow oldCow = createCow("VACA-032", "Nina", CowStatus.DENTRO);

        createAlert(recentCow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "Reciente", FIXED_NOW.minusMinutes(10));
        createAlert(mediumCow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "Media", FIXED_NOW.minusMinutes(30));
        createAlert(oldCow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "Antigua", FIXED_NOW.minusHours(2));
        createAlert(oldCow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "Muy antigua", FIXED_NOW.minusHours(7));

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/dashboard/pending-alert-aging")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAlerts").value(4))
                .andExpect(jsonPath("$.olderThan15Minutes").value(3))
                .andExpect(jsonPath("$.olderThan1Hour").value(2))
                .andExpect(jsonPath("$.olderThan6Hours").value(1));
    }

    @Test
    void shouldExposeTelemetryFreshnessKpiOnDashboard() throws Exception {
        Cow withinThresholdCow = createCow("VACA-040", "Rio", CowStatus.DENTRO);
        Cow staleCow = createCow("VACA-041", "Loma", CowStatus.DENTRO);
        Cow veryStaleCow = createCow("VACA-042", "Duna", CowStatus.DENTRO);
        Cow neverReportedCow = createCow("VACA-043", "Aura", CowStatus.DENTRO);

        createCollar("COL-040", withinThresholdCow, FIXED_NOW.minusMinutes(5));
        createCollar("COL-041", staleCow, FIXED_NOW.minusMinutes(20));
        createCollar("COL-042", veryStaleCow, FIXED_NOW.minusHours(7));
        createCollar("COL-043", neverReportedCow, null);

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/dashboard/telemetry-freshness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabledCollars").value(4))
                .andExpect(jsonPath("$.neverReported").value(1))
                .andExpect(jsonPath("$.reportingWithinThreshold").value(1))
                .andExpect(jsonPath("$.lastSeenOlderThanThreshold").value(2))
                .andExpect(jsonPath("$.lastSeenOlderThan1Hour").value(1))
                .andExpect(jsonPath("$.lastSeenOlderThan6Hours").value(1))
                .andExpect(jsonPath("$.operationalThresholdMinutes").value(15));
    }

    @Test
    void shouldExposeTopProblematicCowsOnDashboard() throws Exception {
        Cow topCow = createCow("VACA-050", "Niebla", CowStatus.DENTRO);
        Cow secondCow = createCow("VACA-051", "Selva", CowStatus.DENTRO);

        createAlert(topCow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "A1", FIXED_NOW.minusHours(3));
        createAlert(topCow, AlertType.COLLAR_OFFLINE, AlertStatus.RESUELTA, "A2", FIXED_NOW.minusHours(2));
        createAlert(topCow, AlertType.EXIT_GEOFENCE, AlertStatus.DESCARTADA, "A3", FIXED_NOW.minusHours(1));

        createAlert(secondCow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "B1", FIXED_NOW.minusHours(4));
        createAlert(secondCow, AlertType.EXIT_GEOFENCE, AlertStatus.RESUELTA, "B2", FIXED_NOW.minusMinutes(30));

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/dashboard/top-problematic-cows")
                        .param("limit", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-050"))
                .andExpect(jsonPath("$[0].totalIncidents").value(3))
                .andExpect(jsonPath("$[0].pendingIncidents").value(1))
                .andExpect(jsonPath("$[0].resolvedIncidents").value(1))
                .andExpect(jsonPath("$[0].discardedIncidents").value(1))
                .andExpect(jsonPath("$[1].cowToken").value("VACA-051"))
                .andExpect(jsonPath("$[1].totalIncidents").value(2));
    }

    private void createUser(String name, String email, String rawPassword, Role role, boolean active) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(active);
        userRepository.save(user);
    }

    private Cow createCow(String token, String name, CowStatus status) {
        Cow cow = new Cow();
        cow.setToken(token);
        cow.setInternalCode("INT-" + token);
        cow.setName(name);
        cow.setStatus(status);
        return cowRepository.save(cow);
    }

    private Alert createAlert(Cow cow,
                              AlertType type,
                              AlertStatus status,
                              String message,
                              LocalDateTime createdAt) {
        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setType(type);
        alert.setStatus(status);
        alert.setMessage(message);
        alert.setCreatedAt(createdAt);
        return alertRepository.save(alert);
    }

    private Collar createCollar(String token, Cow cow, LocalDateTime lastSeenAt) {
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setCow(cow);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(true);
        collar.setSignalStatus(lastSeenAt == null ? DeviceSignalStatus.SIN_SENAL : DeviceSignalStatus.FUERTE);
        collar.setLastSeenAt(lastSeenAt);
        return collarRepository.save(collar);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = json.get("token").asText();
        assertTrue(token != null && !token.isBlank());
        return token;
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return FIXED_CLOCK;
        }
    }
}
