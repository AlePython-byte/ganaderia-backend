package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.CollarRepository;
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

class OfflineCollarReportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        collarRepository.deleteAll();
        cowRepository.deleteAll();
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
        createUser("Supervisor", "supervisor@test.com", "12345678", Role.SUPERVISOR, true);
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
    }

    @Test
    void shouldReturnOnlyEnabledOfflineCollarsForAdmin() throws Exception {
        Cow cow1 = createCow("VACA-OFFREP-001", "Luna");
        Cow cow2 = createCow("VACA-OFFREP-002", "Canela");
        Cow cow3 = createCow("VACA-OFFREP-003", "Estrella");

        createCollar("COLLAR-OFFREP-001", cow1, true, DeviceSignalStatus.SIN_SENAL, 70, LocalDateTime.now().minusMinutes(40));
        createCollar("COLLAR-OFFREP-002", cow2, true, DeviceSignalStatus.MEDIA, 80, LocalDateTime.now().minusMinutes(5));
        createCollar("COLLAR-OFFREP-003", cow3, false, DeviceSignalStatus.SIN_SENAL, 60, LocalDateTime.now().minusMinutes(50));

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/reports/offline-collars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].collarToken").value("COLLAR-OFFREP-001"))
                .andExpect(jsonPath("$[0].signalStatus").value("SIN_SENAL"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].cowToken").value("VACA-OFFREP-001"));
    }

    @Test
    void shouldAllowSupervisorToAccessOfflineCollarReport() throws Exception {
        Cow cow = createCow("VACA-OFFREP-004", "Nube");
        createCollar("COLLAR-OFFREP-004", cow, true, DeviceSignalStatus.SIN_SENAL, 55, LocalDateTime.now().minusMinutes(30));

        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/reports/offline-collars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldDenyOperatorAccessToOfflineCollarReport() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/reports/offline-collars")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/reports/offline-collars"));
    }

    @Test
    void shouldReturnOfflineCollarsStalenessReportOrderedByOperationalSeverity() throws Exception {
        Cow cow1 = createCow("VACA-OFFREP-005", "Nube");
        Cow cow2 = createCow("VACA-OFFREP-006", "Brisa");
        Cow cow3 = createCow("VACA-OFFREP-007", "Sol");

        createCollar("COLLAR-OFFREP-005", cow1, true, DeviceSignalStatus.SIN_SENAL, 55, null);
        createCollar("COLLAR-OFFREP-006", cow2, true, DeviceSignalStatus.SIN_SENAL, 60, LocalDateTime.now().minusHours(7));
        createCollar("COLLAR-OFFREP-007", cow3, true, DeviceSignalStatus.SIN_SENAL, 65, LocalDateTime.now().minusMinutes(35));

        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/reports/offline-collars/staleness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].collarToken").value("COLLAR-OFFREP-005"))
                .andExpect(jsonPath("$[0].stalenessBucket").value("NEVER_REPORTED"))
                .andExpect(jsonPath("$[1].collarToken").value("COLLAR-OFFREP-006"))
                .andExpect(jsonPath("$[1].stalenessBucket").value("OFFLINE_GT_6H"))
                .andExpect(jsonPath("$[2].collarToken").value("COLLAR-OFFREP-007"))
                .andExpect(jsonPath("$[2].stalenessBucket").value("OFFLINE_GT_THRESHOLD"));
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

    private Collar createCollar(String token,
                                Cow cow,
                                boolean enabled,
                                DeviceSignalStatus signalStatus,
                                int batteryLevel,
                                LocalDateTime lastSeenAt) {
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setCow(cow);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(enabled);
        collar.setSignalStatus(signalStatus);
        collar.setBatteryLevel(batteryLevel);
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
        return json.get("token").asText();
    }
}
