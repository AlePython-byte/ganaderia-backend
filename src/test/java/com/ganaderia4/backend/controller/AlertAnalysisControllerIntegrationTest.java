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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertAnalysisControllerIntegrationTest extends AbstractIntegrationTest {

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
    void shouldRejectSummaryWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/alert-analysis/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/alert-analysis/summary"));
    }

    @Test
    void shouldAllowOperatorToGetAlertAnalysisSummary() throws Exception {
        Cow cow = createCow("VACA-AN-001", "Luna");
        createAlert(cow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "Collar sin reporte");
        createAlert(cow, AlertType.LOW_BATTERY, AlertStatus.PENDIENTE, "Bateria baja");

        mockMvc.perform(get("/api/alert-analysis/summary")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.totalPendingAlerts").value(2))
                .andExpect(jsonPath("$.confidence").value("RULE_BASED"))
                .andExpect(jsonPath("$.criticalSignals", hasSize(3)))
                .andExpect(jsonPath("$.recommendedActions").isArray());
    }

    @Test
    void shouldRejectTopPrioritiesWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/alert-analysis/top-priorities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/alert-analysis/top-priorities"));
    }

    @Test
    void shouldAllowOperatorToGetTopPriorities() throws Exception {
        Cow cow = createCow("VACA-AN-002", "Brisa");
        createAlert(cow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE, "Salida geocerca");
        createAlert(cow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "Collar sin reporte");

        mockMvc.perform(get("/api/alert-analysis/top-priorities")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alertStatus").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].priorityScore").isNumber())
                .andExpect(jsonPath("$[0].reason").isString())
                .andExpect(jsonPath("$[0].recommendedAction").isString());
    }

    @Test
    void shouldRejectTopPrioritiesWhenLimitIsInvalid() throws Exception {
        mockMvc.perform(get("/api/alert-analysis/top-priorities")
                        .param("limit", "0")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El limit debe estar entre 1 y 20"))
                .andExpect(jsonPath("$.path").value("/api/alert-analysis/top-priorities"));
    }

    @Test
    void shouldRejectAiSummaryWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/alert-analysis/ai-summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/alert-analysis/ai-summary"));
    }

    @Test
    void shouldAllowOperatorToGetAiSummaryWithFallbackWhenAiIsDisabled() throws Exception {
        Cow cow = createCow("VACA-AN-003", "Canela");
        createAlert(cow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, "Collar sin reporte");

        mockMvc.perform(get("/api/alert-analysis/ai-summary")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.source").value("RULE_BASED_FALLBACK"))
                .andExpect(jsonPath("$.fallbackUsed").value(true))
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.recommendation").isString());
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

    private void createAlert(Cow cow, AlertType type, AlertStatus status, String message) {
        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setType(type);
        alert.setStatus(status);
        alert.setMessage(message);
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        alertRepository.save(alert);
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
