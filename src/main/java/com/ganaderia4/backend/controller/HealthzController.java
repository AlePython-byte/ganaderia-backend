package com.ganaderia4.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Salud del sistema", description = "Endpoints ligeros de verificacion de disponibilidad")
public class HealthzController {

    @GetMapping("/healthz")
    @Operation(
            summary = "Health check ligero",
            description = "Endpoint publico y liviano para verificar disponibilidad basica del backend. No requiere JWT y no reemplaza chequeos detallados de actuator."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backend disponible",
                    content = @Content(schema = @Schema(example = "{\"status\":\"ok\"}")))
    })
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }
}
