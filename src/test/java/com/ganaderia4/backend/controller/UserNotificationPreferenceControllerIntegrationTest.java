package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserNotificationPreferenceRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserNotificationPreferenceControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserNotificationPreferenceRepository preferenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long adminUserId;
    private Long supervisorUserId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        userRepository.deleteAll();

        adminUserId = createUser("Administrador", "admin@test.com", Role.ADMINISTRADOR).getId();
        supervisorUserId = createUser("Supervisor", "supervisor@test.com", Role.SUPERVISOR).getId();
        targetUserId = createUser("Operador", "operador@test.com", Role.OPERADOR).getId();
    }

    @Test
    void shouldCreateDefaultPreferencesWhenGettingForFirstTime() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(targetUserId))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.smsEnabled").value(false))
                .andExpect(jsonPath("$.notificationEmail").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.minimumSeverity").value("MEDIUM"));

        assertEquals(1, preferenceRepository.count());
        assertNotNull(preferenceRepository.findByUserId(targetUserId).orElseThrow().getId());
    }

    @Test
    void shouldNotDuplicatePreferencesForSameUser() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertEquals(1, preferenceRepository.count());
    }

    @Test
    void shouldUpdatePreferencesAsAdmin() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        String payload = """
                {
                  "emailEnabled": false,
                  "smsEnabled": true,
                  "notificationEmail": "  alerts@ganaderia.test  ",
                  "phoneNumber": " +57 300-123-4567 ",
                  "minimumSeverity": "HIGH"
                }
                """;

        mockMvc.perform(put("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.smsEnabled").value(true))
                .andExpect(jsonPath("$.notificationEmail").value("alerts@ganaderia.test"))
                .andExpect(jsonPath("$.phoneNumber").value("+57 300-123-4567"))
                .andExpect(jsonPath("$.minimumSeverity").value("HIGH"));
    }

    @Test
    void shouldRejectInvalidNotificationEmail() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        String payload = """
                {
                  "emailEnabled": true,
                  "smsEnabled": false,
                  "notificationEmail": "correo-invalido",
                  "phoneNumber": null,
                  "minimumSeverity": "MEDIUM"
                }
                """;

        mockMvc.perform(put("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El correo de notificacion no tiene un formato valido"));
    }

    @Test
    void shouldRejectSmsEnabledWithoutPhoneNumber() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        String payload = """
                {
                  "emailEnabled": true,
                  "smsEnabled": true,
                  "notificationEmail": null,
                  "phoneNumber": null,
                  "minimumSeverity": "MEDIUM"
                }
                """;

        mockMvc.perform(put("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("phoneNumber es obligatorio cuando smsEnabled=true"));
    }

    @Test
    void shouldRejectInvalidMinimumSeverity() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        String payload = """
                {
                  "emailEnabled": true,
                  "smsEnabled": false,
                  "notificationEmail": null,
                  "phoneNumber": null,
                  "minimumSeverity": "URGENT"
                }
                """;

        mockMvc.perform(put("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("minimumSeverity debe ser uno de LOW, MEDIUM, HIGH o CRITICAL"));
    }

    @Test
    void shouldRejectEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/notification-preferences", targetUserId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/users/" + targetUserId + "/notification-preferences"));
    }

    @Test
    void shouldRejectSupervisorAccess() throws Exception {
        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/users/{userId}/notification-preferences", targetUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/users/" + targetUserId + "/notification-preferences"));
    }

    private User createUser(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("12345678"));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
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
