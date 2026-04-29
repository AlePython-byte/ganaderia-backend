package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AuditLogResponseDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Auditoria", description = "Consulta de eventos de auditoria operativa y administrativa")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Obtener eventos de auditoria recientes",
            description = "Devuelve los ultimos 100 eventos de auditoria ordenados por timestamp descendente. Permite filtrar opcionalmente por action. Los registros incluyen actor, action, resource type, resource id, source, details, timestamp y success."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos de auditoria obtenidos correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuditLogResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Filtro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AuditLogResponseDTO> getRecentLogs(
            @Parameter(description = "Filtro opcional por action exacta, por ejemplo CREATE_USER o REGISTER_DEVICE_LOCATION")
            @RequestParam(required = false) String action) {
        if (action != null && !action.isBlank()) {
            return auditLogService.getRecentLogsByAction(action);
        }

        return auditLogService.getRecentLogs();
    }
}
