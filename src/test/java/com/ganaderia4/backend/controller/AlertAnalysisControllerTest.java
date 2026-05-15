package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AlertAiSummaryDTO;
import com.ganaderia4.backend.exception.GlobalExceptionHandler;
import com.ganaderia4.backend.exception.TooManyRequestsException;
import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import com.ganaderia4.backend.security.AiAnalysisAbuseProtectionService;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.service.AlertAiAnalysisService;
import com.ganaderia4.backend.service.AlertAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertAnalysisControllerTest {

    private MockMvc mockMvc;
    private AlertAiAnalysisService alertAiAnalysisService;
    private AiAnalysisAbuseProtectionService aiAnalysisAbuseProtectionService;

    @BeforeEach
    void setUp() {
        AlertAnalysisService alertAnalysisService = mock(AlertAnalysisService.class);
        alertAiAnalysisService = mock(AlertAiAnalysisService.class);
        aiAnalysisAbuseProtectionService = mock(AiAnalysisAbuseProtectionService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");

        mockMvc = MockMvcBuilders.standaloneSetup(new AlertAnalysisController(
                        alertAnalysisService,
                        alertAiAnalysisService,
                        aiAnalysisAbuseProtectionService,
                        clientIpResolver
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCallAiSummaryWhenRateLimitAllowsRequest() throws Exception {
        when(alertAiAnalysisService.getAiSummary()).thenReturn(new AlertAiSummaryDTO(
                AlertAnalysisRiskLevel.LOW,
                "Sin alertas relevantes.",
                "Continuar monitoreo.",
                "RULE_BASED_FALLBACK",
                true
        ));

        mockMvc.perform(get("/api/alert-analysis/ai-summary")
                        .principal(new UsernamePasswordAuthenticationToken("operador@test.com", "n/a")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.source").value("RULE_BASED_FALLBACK"));

        verify(aiAnalysisAbuseProtectionService).recordAiSummaryRequest("operador@test.com", "127.0.0.1");
        verify(alertAiAnalysisService).getAiSummary();
    }

    @Test
    void shouldNotCallAiSummaryServiceWhenRateLimitBlocksRequest() throws Exception {
        doThrow(new TooManyRequestsException("Demasiadas solicitudes. Intenta nuevamente mas tarde", 300))
                .when(aiAnalysisAbuseProtectionService)
                .recordAiSummaryRequest(eq("operador@test.com"), eq("127.0.0.1"));

        mockMvc.perform(get("/api/alert-analysis/ai-summary")
                        .principal(new UsernamePasswordAuthenticationToken("operador@test.com", "n/a"))
                        .header("X-Request-Id", "ai-summary-unit-rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.requestId").value("ai-summary-unit-rate-limit"));

        verify(alertAiAnalysisService, never()).getAiSummary();
    }
}
