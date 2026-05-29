package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertAnalysisRequest;
import com.ganaderia4.backend.dto.AlertAnalysisResponse;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.service.DeepSeekService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@Tag(
        name = "IA / Análisis Operativo",
        description = "Análisis inteligente de alertas ganaderas usando DeepSeek AI — " +
                      "diagnóstico narrativo, detección de riesgos y recomendaciones operativas"
)
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class DeepSeekAlertController {

    private final DeepSeekService deepSeekService;

    public DeepSeekAlertController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Analizar alertas con DeepSeek AI",
            description = """
                    Recibe el estado operativo actual del hato (alertas activas, nivel de riesgo, \
                    eventos críticos y problemas identificados) y genera un diagnóstico narrativo \
                    en español usando DeepSeek AI.

                    El análisis incluye:
                    - Descripción del estado operativo actual del hato
                    - Identificación de los riesgos principales basada en los datos enviados
                    - Recomendaciones concretas y accionables para el operador

                    **Requisitos:**
                    - Autenticación JWT Bearer obligatoria
                    - La variable de entorno `DEEPSEEK_API_KEY` debe estar configurada en el servidor

                    **Límites de entrada:**
                    - `activeAlerts`: número entero ≥ 0
                    - `riskLevel`: exactamente `LOW`, `MEDIUM` o `HIGH`
                    - `criticalEvents`: lista de hasta 50 eventos, cada uno máximo 300 caracteres
                    - `problems`: lista de hasta 50 problemas, cada uno máximo 300 caracteres
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Análisis generado correctamente por DeepSeek AI",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertAnalysisResponse.class),
                            examples = @ExampleObject(
                                    name = "Análisis con riesgo alto",
                                    summary = "Respuesta típica con nivel HIGH y collar sin señal",
                                    value = """
                                            {
                                              "analysis": "El hato presenta un nivel de riesgo ALTO con 7 alertas \
activas. La vaca 42 lleva más de 2 horas fuera de la geocerca asignada, lo que puede indicar una ruptura en el \
cerco perimetral o un desplazamiento deliberado del animal. El collar 15 sin señal GPS desde las 06:30 impide \
el monitoreo de ese individuo. Las condiciones de temperatura superior a 35°C y el déficit hídrico en el pozo \
principal agravan el riesgo de estrés calórico en el hato. Acción prioritaria: inspeccionar el cerco perimetral \
del sector donde fue visto por última vez el animal 42, restablecer la conexión del collar 15 y garantizar \
el suministro de agua fresca.",
                                              "provider": "DEEPSEEK",
                                              "generatedAt": "2026-05-29T14:32:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Solicitud inválida. Puede ocurrir cuando:
                            - Algún campo obligatorio está ausente o nulo
                            - `riskLevel` no es `LOW`, `MEDIUM` o `HIGH`
                            - `activeAlerts` es negativo
                            - Algún elemento de las listas supera 300 caracteres
                            - La API key de DeepSeek no está configurada en el servidor
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "riskLevel inválido",
                                            summary = "El valor enviado en riskLevel no es reconocido",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "code": "VALIDATION_ERROR",
                                                      "message": "riskLevel debe ser LOW, MEDIUM o HIGH",
                                                      "path": "/api/alerts/analyze",
                                                      "timestamp": "2026-05-29T14:31:00"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "API key no configurada",
                                            summary = "La variable de entorno DEEPSEEK_API_KEY está vacía",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "code": "BAD_REQUEST",
                                                      "message": "El servicio de análisis IA no está configurado.",
                                                      "path": "/api/alerts/analyze",
                                                      "timestamp": "2026-05-29T14:31:00"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, expirado o inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene permisos para acceder a este recurso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = """
                            DeepSeek AI no disponible. Puede ocurrir cuando:
                            - La API de DeepSeek devuelve un error HTTP (4xx/5xx)
                            - Falla de conectividad de red hacia `api.deepseek.com`
                            - Se agota el tiempo de espera (timeout configurado en el servidor)
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Proveedor IA no disponible",
                                    value = """
                                            {
                                              "status": 503,
                                              "error": "Service Unavailable",
                                              "code": "AI_PROVIDER_UNAVAILABLE",
                                              "message": "El servicio de análisis IA no está disponible en este momento. Intente nuevamente más tarde.",
                                              "path": "/api/alerts/analyze",
                                              "timestamp": "2026-05-29T14:31:00"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<AlertAnalysisResponse> analyze(
            @RequestBody(
                    description = "Estado operativo actual del hato: alertas activas, nivel de riesgo, " +
                                  "eventos críticos y problemas identificados",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AlertAnalysisRequest.class),
                            examples = @ExampleObject(
                                    name = "Hato con riesgo alto",
                                    summary = "7 alertas activas, collar sin señal y estrés hídrico",
                                    value = """
                                            {
                                              "activeAlerts": 7,
                                              "riskLevel": "HIGH",
                                              "criticalEvents": [
                                                "Vaca 42 fuera de geocerca por más de 2 horas",
                                                "Collar 15 sin señal GPS desde las 06:30"
                                              ],
                                              "problems": [
                                                "Temperatura ambiente superior a 35°C",
                                                "Pozo de agua principal sin nivel mínimo"
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AlertAnalysisRequest request) {
        AlertAnalysisResponse response = deepSeekService.analyzeAlerts(request);
        return ResponseEntity.ok(response);
    }
}
