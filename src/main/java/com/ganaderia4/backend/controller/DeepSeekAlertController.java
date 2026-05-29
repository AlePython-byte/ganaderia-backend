package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertAnalysisRequest;
import com.ganaderia4.backend.dto.AlertAnalysisResponse;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.service.DeepSeekService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alert Analysis (DeepSeek)", description = "Análisis inteligente de alertas ganaderas usando DeepSeek AI")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class DeepSeekAlertController {

    private final DeepSeekService deepSeekService;

    public DeepSeekAlertController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/analyze")
    @Operation(
            summary = "Analizar alertas con DeepSeek AI",
            description = "Recibe el estado operativo actual del hato y genera un diagnóstico narrativo " +
                          "usando DeepSeek AI. Requiere que la API key esté configurada en el servidor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Análisis generado correctamente",
                    content = @Content(schema = @Schema(implementation = AlertAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o API key no configurada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error al comunicarse con el proveedor de IA",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AlertAnalysisResponse> analyze(@Valid @RequestBody AlertAnalysisRequest request) {
        AlertAnalysisResponse response = deepSeekService.analyzeAlerts(request);
        return ResponseEntity.ok(response);
    }
}
