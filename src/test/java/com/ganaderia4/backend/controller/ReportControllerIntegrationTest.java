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

class ReportControllerIntegrationTest extends AbstractIntegrationTest {

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
    void shouldAllowAdminToFilterAlertsByStatusAndType() throws Exception {
        Cow cow = createCow("VACA-REP-001", "Luna");

        createAlert(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Collar sin señal",
                LocalDateTime.now().minusHours(2)
        );

        createAlert(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE,
                "Salida de geocerca",
                LocalDateTime.now().minusHours(1)
        );

        createAlert(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.RESUELTA,
                "Collar recuperado",
                LocalDateTime.now().minusMinutes(30)
        );

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/reports/alerts")
                        .param("type", "COLLAR_OFFLINE")
                        .param("status", "PENDIENTE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("COLLAR_OFFLINE"))
                .andExpect(jsonPath("$[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-REP-001"));
    }

    @Test
    void shouldAllowSupervisorToFilterAlertsByDateRange() throws Exception {
        Cow cow = createCow("VACA-REP-002", "Canela");

        createAlert(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Muy vieja",
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );

        createAlert(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE,
                "Dentro del rango",
                LocalDateTime.of(2026, 4, 10, 10, 0)
        );

        createAlert(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.RESUELTA,
                "También dentro del rango",
                LocalDateTime.of(2026, 4, 11, 12, 0)
        );

        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/reports/alerts")
                        .param("from", "2026-04-10T00:00:00")
                        .param("to", "2026-04-11T23:59:59")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldDenyOperatorAccessToReports() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/reports/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/reports/alerts"));
    }

    @Test
    void shouldRejectAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/reports/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("No autorizado"))
                .andExpect(jsonPath("$.path").value("/api/reports/alerts"));
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
        Cow cow = new Cow();
        cow.setToken(token);
        cow.setInternalCode("INT-" + token);
        cow.setName(name);
        cow.setStatus(CowStatus.DENTRO);
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