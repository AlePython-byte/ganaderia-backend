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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerIntegrationTest extends AbstractIntegrationTest {

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
                LocalDateTime.now().minusHours(2)
        );
        createAlert(
                lowerPriorityCow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Collar sin reporte reciente",
                LocalDateTime.now().minusMinutes(5)
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
                LocalDateTime.now().minusHours(2)
        );
        createAlert(
                lowerPriorityCow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Collar sin reporte reciente",
                LocalDateTime.now().minusMinutes(5)
        );

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/dashboard/critical-alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXIT_GEOFENCE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].priorityScore").value(80));
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
}
