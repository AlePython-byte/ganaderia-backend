package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.dto.DashboardSummaryDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.dto.PendingAlertAgingDTO;
import com.ganaderia4.backend.dto.TelemetryFreshnessDTO;
import com.ganaderia4.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores operativos, colas de atencion y vistas resumidas para monitoreo del sistema")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Obtener resumen operativo",
            description = "Devuelve el resumen principal del dashboard: conteos de vacas, collares, alertas pendientes y ultima telemetria registrada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen operativo obtenido correctamente",
                    content = @Content(schema = @Schema(implementation = DashboardSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public DashboardSummaryDTO getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/critical-alerts")
    @Operation(
            summary = "Obtener alertas criticas",
            description = "Alias de compatibilidad del dashboard que devuelve una vista corta de la cola priorizada de alertas, limitada a los casos mas urgentes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alertas criticas obtenidas correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertResponseDTO> getCriticalAlerts() {
        return dashboardService.getCriticalAlerts();
    }

    @GetMapping("/prioritized-alert-queue")
    @Operation(
            summary = "Obtener cola priorizada de alertas",
            description = "Devuelve alertas pendientes ordenadas por prioridad operativa para triage. Es la vista recomendada para la bandeja principal de atencion."
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
    public List<AlertResponseDTO> getPrioritizedAlertQueue(
            @Parameter(description = "Cantidad maxima de alertas a devolver", example = "20")
            @RequestParam(required = false) Integer limit
    ) {
        return dashboardService.getPrioritizedAlertQueue(limit);
    }

    @GetMapping("/pending-alert-aging")
    @Operation(
            summary = "Obtener aging de alertas pendientes",
            description = "Resume el envejecimiento de alertas pendientes por ventanas operativas para identificar backlog y demoras de atencion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aging de alertas obtenido correctamente",
                    content = @Content(schema = @Schema(implementation = PendingAlertAgingDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public PendingAlertAgingDTO getPendingAlertAging() {
        return dashboardService.getPendingAlertAging();
    }

    @GetMapping("/telemetry-freshness")
    @Operation(
            summary = "Obtener frescura de telemetria",
            description = "Resume la frescura operativa de la telemetria de collares usando el umbral offline configurado por el backend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frescura de telemetria obtenida correctamente",
                    content = @Content(schema = @Schema(implementation = TelemetryFreshnessDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public TelemetryFreshnessDTO getTelemetryFreshness() {
        return dashboardService.getTelemetryFreshness();
    }

    @GetMapping("/top-problematic-cows")
    @Operation(
            summary = "Obtener vacas con mas incidentes",
            description = "Devuelve un ranking operativo de vacas con mas incidencias acumuladas para apoyar priorizacion de seguimiento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking de vacas problematicas obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CowIncidentReportDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Limite invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CowIncidentReportDTO> getTopProblematicCows(
            @Parameter(description = "Cantidad maxima de vacas a devolver", example = "10")
            @RequestParam(required = false) Integer limit
    ) {
        return dashboardService.getTopProblematicCows(limit);
    }

    @GetMapping("/collars-offline")
    @Operation(
            summary = "Obtener collares offline",
            description = "Lista collares actualmente sin senal para seguimiento operativo rapido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de collares offline obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollarResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CollarResponseDTO> getOfflineCollars() {
        return dashboardService.getOfflineCollars();
    }

    @GetMapping("/cows-outside-geofence")
    @Operation(
            summary = "Obtener vacas fuera de geocerca",
            description = "Lista vacas marcadas actualmente fuera de geocerca para respuesta operativa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de vacas fuera de geocerca obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CowResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CowResponseDTO> getCowsOutsideGeofence() {
        return dashboardService.getCowsOutsideGeofence();
    }

    @GetMapping("/recent-locations")
    @Operation(
            summary = "Obtener ubicaciones recientes",
            description = "Devuelve las ubicaciones mas recientes registradas para la vista operativa del mapa o timeline."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ubicaciones recientes obtenidas correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LocationResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<LocationResponseDTO> getRecentLocations() {
        return dashboardService.getRecentLocations();
    }
}
