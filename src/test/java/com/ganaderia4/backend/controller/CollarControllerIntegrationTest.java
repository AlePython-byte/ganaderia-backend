package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CollarControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        collarRepository.deleteAll();
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
        createUser("Supervisor", "supervisor@test.com", "12345678", Role.SUPERVISOR, true);
        createUser("Tecnico", "tecnico@test.com", "12345678", Role.TECNICO, true);
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
    }

    @Test
    void shouldRejectCreatingCollarWhenTokenFormatIsInvalid() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(post("/api/collars")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "COLLAR/001",
                                  "status": "ACTIVO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El token del collar solo puede contener letras, numeros, punto, guion y guion bajo"))
                .andExpect(jsonPath("$.path").value("/api/collars"));
    }

    @Test
    void shouldRejectUpdatingCollarWhenTokenChanges() throws Exception {
        Collar collar = new Collar();
        collar.setToken("COLLAR-001");
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(true);
        Collar savedCollar = collarRepository.save(collar);

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(put("/api/collars/{id}", savedCollar.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "COLLAR-002",
                                  "status": "ACTIVO",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El token del collar es un identificador publico estable y no puede modificarse"))
                .andExpect(jsonPath("$.path").value("/api/collars/" + savedCollar.getId()));
    }

    @Test
    void shouldAllowAdminToRotateDeviceSecret() throws Exception {
        Collar savedCollar = createCollar("COLLAR-ROTATE-001");
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(patch("/api/collars/{id}/rotate-secret", savedCollar.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenySupervisorFromRotatingDeviceSecret() throws Exception {
        Collar savedCollar = createCollar("COLLAR-ROTATE-002");
        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(patch("/api/collars/{id}/rotate-secret", savedCollar.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/collars/" + savedCollar.getId() + "/rotate-secret"));
    }

    @Test
    void shouldDenyTecnicoFromRotatingDeviceSecret() throws Exception {
        Collar savedCollar = createCollar("COLLAR-ROTATE-003");
        String token = loginAndGetToken("tecnico@test.com", "12345678");

        mockMvc.perform(patch("/api/collars/{id}/rotate-secret", savedCollar.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/collars/" + savedCollar.getId() + "/rotate-secret"));
    }

    @Test
    void shouldDenyOperadorFromRotatingDeviceSecret() throws Exception {
        Collar savedCollar = createCollar("COLLAR-ROTATE-004");
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(patch("/api/collars/{id}/rotate-secret", savedCollar.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/collars/" + savedCollar.getId() + "/rotate-secret"));
    }

    @Test
    void shouldRejectRotateDeviceSecretWhenTokenIsMissing() throws Exception {
        Collar savedCollar = createCollar("COLLAR-ROTATE-005");

        mockMvc.perform(patch("/api/collars/{id}/rotate-secret", savedCollar.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/collars/" + savedCollar.getId() + "/rotate-secret"));
    }

    private Collar createCollar(String collarToken) {
        Collar collar = new Collar();
        collar.setToken(collarToken);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(true);
        return collarRepository.save(collar);
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
