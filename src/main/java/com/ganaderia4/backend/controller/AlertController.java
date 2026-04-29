package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertUpdateRequestDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alertas", description = "Consulta y gestion operativa de alertas del sistema")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar todas las alertas (legacy)",
            description = "Endpoint legacy sin paginacion. Se mantiene por compatibilidad y se recomienda usar /api/alerts/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de alertas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "La consulta legacy supera el limite permitido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertResponseDTO> getAllAlerts() {
        return alertService.getAllAlertsLegacy();
    }

    @GetMapping("/page")
    @Operation(
            summary = "Listar alertas paginadas",
            description = "Consulta paginada de alertas con filtros por estado y tipo. Los registros pendientes se ordenan segun el criterio solicitado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de alertas obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacion o filtro invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<AlertResponseDTO> getAlertsPage(
            @Parameter(description = "Filtro opcional por estado de la alerta")
            @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false) AlertType type,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @Parameter(description = "Campo de ordenamiento permitido. Ejemplos: createdAt, status, type, id", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Direccion de ordenamiento", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        return alertService.getAlertsPage(status, type, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener alerta por id",
            description = "Recupera una alerta por su identificador numerico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta encontrada",
                    content = @Content(schema = @Schema(implementation = AlertResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public AlertResponseDTO getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id);
    }

    @GetMapping("/status/{status}")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar alertas por estado (legacy)",
            description = "Endpoint legacy sin paginacion. Para acceso controlado se recomienda usar /api/alerts/page o /api/alerts/pending/priority-queue.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado filtrado por estado",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "La consulta legacy supera el limite permitido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertResponseDTO> getAlertsByStatus(
            @Parameter(description = "Estado exacto que se desea consultar")
            @PathVariable AlertStatus status) {
        return alertService.getAlertsByStatusLegacy(status);
    }

    @GetMapping("/pending/priority-queue")
    @Operation(
            summary = "Obtener cola priorizada de alertas pendientes",
            description = "Devuelve alertas pendientes priorizadas para atencion operativa. Disponible para roles ADMINISTRADOR, SUPERVISOR, OPERADOR y TECNICO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cola priorizada obtenida correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Limite invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertResponseDTO> getPendingAlertPriorityQueue(
            @Parameter(description = "Cantidad maxima de alertas a devolver", example = "20")
            @RequestParam(required = false) Integer limit
    ) {
        return alertService.getPendingAlertPriorityQueue(limit);
    }

    @GetMapping("/type/{type}")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar alertas por tipo (legacy)",
            description = "Endpoint legacy sin paginacion para filtrar por tipo. Se mantiene por compatibilidad y se recomienda usar /api/alerts/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado filtrado por tipo",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "La consulta legacy supera el limite permitido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertResponseDTO> getAlertsByType(
            @Parameter(description = "Tipo exacto de alerta")
            @PathVariable AlertType type) {
        return alertService.getAlertsByTypeLegacy(type);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Actualizar alerta",
            description = "Actualiza estado y observaciones de una alerta existente. Requiere rol ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta actualizada correctamente",
                    content = @Content(schema = @Schema(implementation = AlertResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido o transicion de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public AlertResponseDTO updateAlert(@PathVariable Long id,
                                        @Valid @RequestBody AlertUpdateRequestDTO requestDTO) {
        return alertService.updateAlert(id, requestDTO);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Resolver alerta",
            description = "Marca una alerta como RESUELTA. Puede registrar observaciones manuales y requiere rol ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta resuelta correctamente",
                    content = @Content(schema = @Schema(implementation = AlertResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Transicion de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public AlertResponseDTO resolveAlert(@PathVariable Long id,
                                         @Parameter(description = "Observaciones opcionales para registrar la resolucion")
                                         @RequestParam(required = false) String observations) {
        return alertService.resolveAlert(id, observations);
    }

    @PatchMapping("/{id}/discard")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Descartar alerta",
            description = "Marca una alerta como DESCARTADA. Puede registrar observaciones manuales y requiere rol ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta descartada correctamente",
                    content = @Content(schema = @Schema(implementation = AlertResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Transicion de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public AlertResponseDTO discardAlert(@PathVariable Long id,
                                         @Parameter(description = "Observaciones opcionales para registrar el descarte")
                                         @RequestParam(required = false) String observations) {
        return alertService.discardAlert(id, observations);
    }
}
