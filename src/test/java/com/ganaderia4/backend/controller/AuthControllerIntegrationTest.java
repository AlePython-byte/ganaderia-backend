package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
        "app.abuse-protection.login.max-attempts=2",
        "app.abuse-protection.login.window=10m",
        "app.abuse-protection.login.block-duration=5m"
})
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AbuseRateLimitRepository abuseRateLimitRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        abuseRateLimitRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setName("Administrador");
        user.setEmail("admin@test.com");
        user.setPassword(passwordEncoder.encode("12345678"));
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);

        userRepository.save(user);
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreCorrect() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("12345678");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso"));
    }

    @Test
    void shouldFailWhenPasswordIsIncorrect() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("claveIncorrecta");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void shouldRateLimitLoginAfterRepeatedFailures() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("claveIncorrecta");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Demasiadas solicitudes. Intenta nuevamente mas tarde"));
    }

    @Test
    void shouldAllowSuccessfulLoginBeforeTemporaryBlockIsReached() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("claveIncorrecta");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        request.setPassword("12345678");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
