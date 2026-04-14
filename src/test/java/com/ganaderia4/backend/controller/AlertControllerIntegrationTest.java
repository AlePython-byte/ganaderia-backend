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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertControllerIntegrationTest extends AbstractIntegrationTest {

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
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
    }

    @Test
    void shouldAllowOperatorToListPendingAlertsByStatus() throws Exception {
        Cow cow = createCow("VACA-001", "Luna");
        createAlert(cow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "La vaca salió de la geocerca", null);
        createAlert(cow, AlertType.COLLAR_OFFLINE, AlertStatus.RESUELTA, "Collar recuperado", "resuelta antes");

        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/alerts/status/PENDIENTE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("EXIT_GEOFENCE"))
                .andExpect(jsonPath("$[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-001"))
                .andExpect(jsonPath("$[0].cowName").value("Luna"));
    }

    @Test
    void shouldAllowAdminToResolveAlert() throws Exception {
        Cow cow = createCow("VACA-002", "Estrella");
        Alert alert = createAlert(cow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "Salida detectada", null);

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(patch("/api/alerts/{id}/resolve", alert.getId())
                        .param("observations", "Caso atendido")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alert.getId()))
                .andExpect(jsonPath("$.status").value("RESUELTA"))
                .andExpect(jsonPath("$.observations").value("Caso atendido"));

        Alert updated = alertRepository.findById(alert.getId()).orElseThrow();
        assertEquals(AlertStatus.RESUELTA, updated.getStatus());
        assertEquals("Caso atendido", updated.getObservations());
    }

    @Test
    void shouldAllowAdminToDiscardAlertWithDefaultObservation() throws Exception {
        Cow cow = createCow("VACA-003", "Canela");
        Alert alert = createAlert(cow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "Sin reporte reciente", null);

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(patch("/api/alerts/{id}/discard", alert.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alert.getId()))
                .andExpect(jsonPath("$.status").value("DESCARTADA"))
                .andExpect(jsonPath("$.observations").value("Alerta descartada manualmente"));

        Alert updated = alertRepository.findById(alert.getId()).orElseThrow();
        assertEquals(AlertStatus.DESCARTADA, updated.getStatus());
        assertEquals("Alerta descartada manualmente", updated.getObservations());
    }

    @Test
    void shouldReturnNotFoundWhenResolvingUnknownAlert() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(patch("/api/alerts/{id}/resolve", 99999L)
                        .param("observations", "no existe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Alerta no encontrada"))
                .andExpect(jsonPath("$.path").value("/api/alerts/99999/resolve"));
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
                              String observations) {
        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setType(type);
        alert.setStatus(status);
        alert.setMessage(message);
        alert.setObservations(observations);
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(5));
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