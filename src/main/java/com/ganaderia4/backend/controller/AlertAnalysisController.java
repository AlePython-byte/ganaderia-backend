package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertAnalysisSummaryDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.service.AlertAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alert-analysis")
@Tag(name = "Alert Analysis", description = "Resumen heuristico y explicable del estado operativo de las alertas")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AlertAnalysisController {

    private final AlertAnalysisService alertAnalysisService;

    public AlertAnalysisController(AlertAnalysisService alertAnalysisService) {
        this.alertAnalysisService = alertAnalysisService;
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
}
