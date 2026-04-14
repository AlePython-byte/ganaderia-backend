package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.observability.RequestCorrelationFilter;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestCorrelationIntegrationTest extends AbstractIntegrationTest {

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

        User user = new User();
        user.setName("Administrador");
        user.setEmail("admin@test.com");
        user.setPassword(passwordEncoder.encode("12345678"));
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);
        userRepository.save(user);
    }

    @Test
    void shouldGenerateRequestIdHeaderWhenMissing() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("12345678");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.HEADER_NAME))
                .andReturn();

        String requestId = result.getResponse().getHeader(RequestCorrelationFilter.HEADER_NAME);

        assertFalse(requestId == null || requestId.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(requestId));
    }

    @Test
    void shouldReuseIncomingRequestIdHeader() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("12345678");

        String requestId = "req-manual-001";

        mockMvc.perform(post("/api/auth/login")
                        .header(RequestCorrelationFilter.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.HEADER_NAME, requestId));
    }

    @Test
    void shouldIncludeRequestIdHeaderOnUnauthorizedResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestCorrelationFilter.HEADER_NAME))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("No autorizado"))
                .andReturn();

        String requestId = result.getResponse().getHeader(RequestCorrelationFilter.HEADER_NAME);

        assertFalse(requestId == null || requestId.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(requestId));
    }

    @Test
    void shouldPreserveProvidedRequestIdOnUnauthorizedResponse() throws Exception {
        String requestId = "req-auth-error-777";

        mockMvc.perform(get("/api/auth/me")
                        .header(RequestCorrelationFilter.HEADER_NAME, requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestCorrelationFilter.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("No autorizado"));
    }
}