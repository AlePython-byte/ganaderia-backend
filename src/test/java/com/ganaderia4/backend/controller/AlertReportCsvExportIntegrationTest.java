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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertReportCsvExportIntegrationTest extends AbstractIntegrationTest {

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
    void shouldExportFilteredAlertsCsvForAdmin() throws Exception {
        Cow cow = createCow("VACA-CSV-001", "Luna");

        createAlert(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                "Collar sin señal",
                LocalDateTime.of(2026, 4, 12, 10, 0)
        );

        createAlert(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.RESUELTA,
                "Salida resuelta",
                LocalDateTime.of(2026, 4, 12, 11, 0)
        );

        String token = loginAndGetToken("admin@test.com", "12345678");

        MvcResult result = mockMvc.perform(get("/api/reports/alerts/export.csv")
                        .param("type", "COLLAR_OFFLINE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=alert-report.csv"))
                .andExpect(content().contentType("text/csv"))
                .andReturn();

        String body = result.getResponse().getContentAsString();

        assertTrue(body.contains("\"id\",\"type\",\"status\",\"message\",\"createdAt\",\"observations\",\"cowId\",\"cowToken\",\"cowName\",\"locationId\"")
                || body.startsWith("id,type,status,message,createdAt,observations,cowId,cowToken,cowName,locationId"));
        assertTrue(body.contains("\"COLLAR_OFFLINE\""));
        assertTrue(body.contains("\"VACA-CSV-001\""));
        assertTrue(!body.contains("\"EXIT_GEOFENCE\""));
    }

    @Test
    void shouldAllowSupervisorToExportAlertsCsv() throws Exception {
        Cow cow = createCow("VACA-CSV-002", "Canela");

        createAlert(
                cow,
                AlertType.EXIT_GEOFENCE,
                AlertStatus.PENDIENTE,
                "Salida detectada",
                LocalDateTime.of(2026, 4, 13, 8, 0)
        );

        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/reports/alerts/export.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    void shouldNeutralizeDangerousFormulaValuesInExportedCsv() throws Exception {
        Cow cow = createCow("VACA-CSV-003", "=@Nombre");

        Alert alert = createAlert(
                cow,
                AlertType.COLLAR_OFFLINE,
                AlertStatus.PENDIENTE,
                " =SUM(1,1)",
                LocalDateTime.of(2026, 4, 14, 8, 0)
        );
        alert.setObservations("+cmd");
        alertRepository.save(alert);

        alertRepository.flush();

        String token = loginAndGetToken("admin@test.com", "12345678");

        MvcResult result = mockMvc.perform(get("/api/reports/alerts/export.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andReturn();

        String body = result.getResponse().getContentAsString();

        assertTrue(body.contains("\"' =SUM(1,1)\""));
        assertTrue(body.contains("\"'+cmd\""));
        assertTrue(body.contains("\"'=@Nombre\""));
    }

    @Test
    void shouldDenyOperatorAccessToCsvExport() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/reports/alerts/export.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/reports/alerts/export.csv"));
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
