package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class SecurityHttpEventLoggingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
    }

    @Test
    void shouldLogHttpUnauthorizedForMissingAuthentication(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        String logs = output.getOut();
        assertTrue(logs.contains("event=http_unauthorized"));
        assertTrue(logs.contains("reason=authentication_required"));
        assertTrue(logs.contains("method=GET"));
        assertTrue(logs.contains("path=/api/auth/me"));
        assertTrue(logs.contains("status=401"));
        assertTrue(logs.contains("requestId="));
    }

    @Test
    void shouldLogHttpForbiddenWithoutExposingFullBearerToken(CapturedOutput output) throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        String logs = output.getOut();
        assertTrue(logs.contains("event=http_forbidden"));
        assertTrue(logs.contains("reason=access_denied"));
        assertTrue(logs.contains("method=GET"));
        assertTrue(logs.contains("path=/api/users"));
        assertTrue(logs.contains("status=403"));
        assertTrue(logs.contains("requestId="));
        assertFalse(logs.contains(token));
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
