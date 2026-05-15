package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertAiSummaryDTO;
import com.ganaderia4.backend.dto.AlertAnalysisSummaryDTO;
import com.ganaderia4.backend.dto.AlertPriorityRecommendationDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.security.AiAnalysisAbuseProtectionService;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.service.AlertAiAnalysisService;
import com.ganaderia4.backend.service.AlertAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alert-analysis")
@Tag(name = "Alert Analysis", description = "Resumen heuristico y explicable del estado operativo de las alertas")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AlertAnalysisController {

    private final AlertAnalysisService alertAnalysisService;
    private final AlertAiAnalysisService alertAiAnalysisService;
    private final AiAnalysisAbuseProtectionService aiAnalysisAbuseProtectionService;
    private final ClientIpResolver clientIpResolver;

    public AlertAnalysisController(AlertAnalysisService alertAnalysisService,
                                   AlertAiAnalysisService alertAiAnalysisService,
                                   AiAnalysisAbuseProtectionService aiAnalysisAbuseProtectionService,
                                   ClientIpResolver clientIpResolver) {
        this.alertAnalysisService = alertAnalysisService;
        this.alertAiAnalysisService = alertAiAnalysisService;
        this.aiAnalysisAbuseProtectionService = aiAnalysisAbuseProtectionService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Obtener resumen heuristico de alertas",
            description = "Devuelve una evaluacion operativa basada en reglas sobre alertas pendientes y señales recientes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de analisis obtenido correctamente",
                    content = @Content(schema = @Schema(implementation = AlertAnalysisSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public AlertAnalysisSummaryDTO getSummary() {
        return alertAnalysisService.getSummary();
    }

    @GetMapping("/top-priorities")
    @Operation(
            summary = "Obtener casos prioritarios de alertas",
            description = "Devuelve alertas pendientes priorizadas por reglas heuristicas y score operativo para facilitar el triage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Casos prioritarios obtenidos correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertPriorityRecommendationDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Limit invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertPriorityRecommendationDTO> getTopPriorities(
            @Parameter(description = "Cantidad maxima de alertas a devolver", example = "5")
            @RequestParam(required = false) Integer limit
    ) {
        return alertAnalysisService.getTopPriorities(limit);
    }

    @GetMapping("/ai-summary")
    @Operation(
            summary = "Obtener resumen operativo asistido por IA",
            description = "Combina el análisis heurístico del backend con una salida narrativa opcional de IA y fallback interno seguro. La respuesta expone riskLevel, summary, recommendation, source y fallbackUsed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen IA o fallback obtenido correctamente",
                    content = @Content(schema = @Schema(implementation = AlertAiSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "429",
                    description = "Se excedio el limite de solicitudes de IA para el usuario o IP actual",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public AlertAiSummaryDTO getAiSummary(Authentication authentication,
                                          HttpServletRequest httpServletRequest) {
        aiAnalysisAbuseProtectionService.recordAiSummaryRequest(
                authentication == null ? null : authentication.getName(),
                clientIpResolver.resolve(httpServletRequest)
        );
        return alertAiAnalysisService.getAiSummary();
    }
}
