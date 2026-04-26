package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CowIncidentReportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        cowRepository.deleteAll();
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
        createUser("Supervisor", "supervisor@test.com", "12345678", Role.SUPERVISOR, true);
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
    }

    @Test
    void shouldReturnCowsOrderedByIncidentCount() throws Exception {
        Cow cow1 = createCow("VACA-INC-001", "Luna");
        Cow cow2 = createCow("VACA-INC-002", "Canela");
        Cow cow3 = createCow("VACA-INC-003", "Estrella");

        createAlert(cow1, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "a1", LocalDateTime.of(2026, 4, 10, 10, 0));
        createAlert(cow1, AlertType.EXIT_GEOFENCE, AlertStatus.RESUELTA, "a2", LocalDateTime.of(2026, 4, 10, 11, 0));
        createAlert(cow1, AlertType.COLLAR_OFFLINE, AlertStatus.DESCARTADA, "a3", LocalDateTime.of(2026, 4, 10, 12, 0));

        createAlert(cow2, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "b1", LocalDateTime.of(2026, 4, 10, 13, 0));
        createAlert(cow2, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "b2", LocalDateTime.of(2026, 4, 10, 14, 0));

        createAlert(cow3, AlertType.COLLAR_OFFLINE, AlertStatus.RESUELTA, "c1", LocalDateTime.of(2026, 4, 10, 15, 0));

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/reports/cows-most-incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-INC-001"))
                .andExpect(jsonPath("$[0].totalIncidents").value(3))
                .andExpect(jsonPath("$[0].pendingIncidents").value(1))
                .andExpect(jsonPath("$[0].resolvedIncidents").value(1))
                .andExpect(jsonPath("$[0].discardedIncidents").value(1))
                .andExpect(jsonPath("$[1].cowToken").value("VACA-INC-002"))
                .andExpect(jsonPath("$[1].totalIncidents").value(2))
                .andExpect(jsonPath("$[2].cowToken").value("VACA-INC-003"))
                .andExpect(jsonPath("$[2].totalIncidents").value(1));
    }

    @Test
    void shouldRespectDateRangeAndLimitFilters() throws Exception {
        Cow cow1 = createCow("VACA-INC-004", "Nube");
        Cow cow2 = createCow("VACA-INC-005", "Brisa");

        createAlert(cow1, AlertType.COLLAR_OFFLINE, AlertStatus.RESUELTA, "old", LocalDateTime.of(2026, 4, 1, 10, 0));
        createAlert(cow1, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "in-range-1", LocalDateTime.of(2026, 4, 11, 10, 0));
        createAlert(cow1, AlertType.EXIT_GEOFENCE, AlertStatus.RESUELTA, "in-range-2", LocalDateTime.of(2026, 4, 11, 11, 0));

        createAlert(cow2, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "in-range-3", LocalDateTime.of(2026, 4, 11, 12, 0));

        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/reports/cows-most-incidents")
                        .param("from", "2026-04-10T00:00:00")
                        .param("to", "2026-04-12T00:00:00")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-INC-004"))
                .andExpect(jsonPath("$[0].totalIncidents").value(2));
    }

    @Test
    void shouldDenyOperatorAccessToCowIncidentsReport() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/reports/cows-most-incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/reports/cows-most-incidents"));
    }

    @Test
    void shouldReturnOperationalCowIncidentRecurrenceOrderedByPendingAndRecency() throws Exception {
        Cow cow1 = createCow("VACA-REC-001", "Luna", CowStatus.FUERA);
        Cow cow2 = createCow("VACA-REC-002", "Canela", CowStatus.DENTRO);

        createAlert(cow1, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "a1", LocalDateTime.of(2026, 4, 12, 10, 0));
        createAlert(cow1, AlertType.EXIT_GEOFENCE, AlertStatus.RESUELTA, "a2", LocalDateTime.of(2026, 4, 11, 10, 0));
        createAlert(cow2, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "b1", LocalDateTime.of(2026, 4, 12, 12, 0));
        createAlert(cow2, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "b2", LocalDateTime.of(2026, 4, 12, 13, 0));

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/reports/cows-incident-recurrence")
                        .param("from", "2026-04-10T00:00:00")
                        .param("to", "2026-04-13T00:00:00")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-REC-002"))
                .andExpect(jsonPath("$[0].pendingIncidents").value(2))
                .andExpect(jsonPath("$[0].cowStatus").value("DENTRO"))
                .andExpect(jsonPath("$[0].lastIncidentType").value("EXIT_GEOFENCE"))
                .andExpect(jsonPath("$[1].cowToken").value("VACA-REC-001"))
                .andExpect(jsonPath("$[1].pendingIncidents").value(1))
                .andExpect(jsonPath("$[1].cowStatus").value("FUERA"));
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

    private Cow createCow(String token, String name) {
        return createCow(token, name, CowStatus.DENTRO);
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
        return json.get("token").asText();
    }
}
