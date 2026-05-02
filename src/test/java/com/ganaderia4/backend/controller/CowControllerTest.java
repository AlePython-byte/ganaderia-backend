package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.exception.GlobalExceptionHandler;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.service.CowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CowControllerTest {

    private MockMvc mockMvc;
    private CowService cowService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cowService = mock(CowService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CowController(cowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateCowWithoutTokenAndReturnGeneratedToken() throws Exception {
        CowResponseDTO response = new CowResponseDTO(1L, "COW-001", "INT-001", "Luna", "DENTRO", null);
        when(cowService.createCow(any())).thenReturn(response);

        String body = """
                {
                  "internalCode": "INT-001",
                  "name": "Luna",
                  "status": "DENTRO"
                }
                """;

        mockMvc.perform(post("/api/cows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("COW-001"))
                .andExpect(jsonPath("$.name").value("Luna"));

        verify(cowService).createCow(any());
    }

    @Test
    void shouldKeepValidatingRequiredCowName() throws Exception {
        String body = """
                {
                  "status": "DENTRO"
                }
                """;

        mockMvc.perform(post("/api/cows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre es obligatorio"));
    }
}
