package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertTrendPointDTO;
import com.ganaderia4.backend.dto.AlertTypeRecurrenceDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.OfflineCollarReportDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertReportService;
import com.ganaderia4.backend.service.CollarReportService;
import com.ganaderia4.backend.service.CowIncidentReportService;
import com.ganaderia4.backend.service.ReportCsvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reportes", description = "Consultas analiticas y exportaciones operativas del backend")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class ReportController {

    private final AlertReportService alertReportService;
    private final CollarReportService collarReportService;
    private final CowIncidentReportService cowIncidentReportService;
    private final ReportCsvService reportCsvService;

    public ReportController(AlertReportService alertReportService,
                            CollarReportService collarReportService,
                            CowIncidentReportService cowIncidentReportService,
                            ReportCsvService reportCsvService) {
        this.alertReportService = alertReportService;
        this.collarReportService = collarReportService;
        this.cowIncidentReportService = cowIncidentReportService;
        this.reportCsvService = reportCsvService;
    }

    @GetMapping("/alerts")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Obtener reporte legacy de alertas",
            description = "Endpoint legacy sin paginacion para consultar alertas por rango temporal, tipo y estado. Se mantiene por compatibilidad y se recomienda usar /api/reports/alerts/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte legacy de alertas obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "La consulta legacy supera el limite permitido o los filtros son invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertResponseDTO> getAlertReport(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return alertReportService.getAlertReportLegacy(filter);
    }

    @GetMapping("/alerts/page")
    @Operation(
            summary = "Obtener reporte paginado de alertas",
            description = "Consulta paginada del reporte de alertas por rango temporal, tipo y estado. Los LocalDateTime del filtro se interpretan como UTC segun la politica temporal del proyecto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina del reporte de alertas obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Filtros o paginacion invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<AlertResponseDTO> getAlertReportPage(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status,

            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}")
            int size,

            @Parameter(description = "Campo de ordenamiento permitido. Ejemplos: createdAt, type, status", example = "createdAt")
            @RequestParam(defaultValue = "createdAt")
            String sort,

            @Parameter(description = "Direccion de ordenamiento", example = "DESC")
            @RequestParam(defaultValue = "DESC")
            String direction
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return alertReportService.getAlertReportPage(filter, page, size, sort, direction);
    }

    @GetMapping("/alerts/trend")
    @Operation(
            summary = "Obtener tendencia de alertas",
            description = "Devuelve puntos agregados de tendencia de alertas para analisis temporal por rango, tipo y estado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tendencia de alertas obtenida correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertTrendPointDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertTrendPointDTO> getAlertTrendReport(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return alertReportService.getAlertTrendReport(filter);
    }

    @GetMapping("/alerts/type-recurrence")
    @Operation(
            summary = "Obtener recurrencia por tipo de alerta",
            description = "Devuelve agregados de recurrencia por tipo de alerta, incluyendo pendientes, resueltas, descartadas y ultima ocurrencia."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recurrencia por tipo obtenida correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertTypeRecurrenceDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<AlertTypeRecurrenceDTO> getAlertTypeRecurrenceReport(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return alertReportService.getAlertTypeRecurrenceReport(filter);
    }

    @GetMapping("/alerts/export.csv")
    @Operation(
            summary = "Exportar reporte de alertas en CSV",
            description = "Exporta el reporte de alertas como text/csv usando los mismos filtros del reporte. El backend mantiene el formato CSV actual y aplica neutralizacion contra CSV Injection sobre celdas potencialmente peligrosas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV generado correctamente",
                    content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "400", description = "Filtros invalidos o cantidad de filas mayor al maximo permitido para exportacion",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<byte[]> exportAlertReportCsv(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        byte[] csvBytes = reportCsvService.exportAlertsReportCsv(filter);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alert-report.csv")
                .body(csvBytes);
    }

    @GetMapping("/offline-collars")
    @Operation(
            summary = "Obtener reporte de collares offline",
            description = "Lista collares sin senal ordenados para seguimiento operativo rapido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de collares offline obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OfflineCollarReportDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<OfflineCollarReportDTO> getOfflineCollarsReport() {
        return collarReportService.getOfflineCollarsReport();
    }

    @GetMapping("/offline-collars/staleness")
    @Operation(
            summary = "Obtener reporte de collares offline por staleness",
            description = "Lista collares sin senal ordenados por antiguedad del ultimo reporte para priorizacion operativa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de staleness de collares offline obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OfflineCollarReportDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<OfflineCollarReportDTO> getOfflineCollarsStalenessReport() {
        return collarReportService.getOfflineCollarsStalenessReport();
    }

    @GetMapping("/cows-most-incidents")
    @Operation(
            summary = "Obtener vacas con mas incidentes",
            description = "Devuelve un ranking de vacas con mayor cantidad de incidentes, filtrable por rango temporal, tipo, estado y limite."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking de vacas con mas incidentes obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CowIncidentReportDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Limite o filtros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CowIncidentReportDTO> getCowsMostIncidentsReport(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status,

            @Parameter(description = "Cantidad maxima de vacas a devolver", example = "10")
            @RequestParam(required = false)
            Integer limit
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return cowIncidentReportService.getCowsMostIncidentsReport(filter, limit);
    }

    @GetMapping("/cows-incident-recurrence")
    @Operation(
            summary = "Obtener recurrencia operativa por vaca",
            description = "Devuelve un ranking de vacas ordenado por recurrencia operativa de incidentes, filtrable por rango temporal, tipo, estado y limite."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking de recurrencia por vaca obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CowIncidentReportDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Limite o filtros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CowIncidentReportDTO> getCowIncidentRecurrenceReport(
            @Parameter(description = "Inicio del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-28T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(description = "Fin del rango sobre createdAt en formato ISO local date-time interpretado como UTC", example = "2026-04-29T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(description = "Filtro opcional por tipo de alerta")
            @RequestParam(required = false)
            AlertType type,

            @Parameter(description = "Filtro opcional por estado de alerta")
            @RequestParam(required = false)
            AlertStatus status,

            @Parameter(description = "Cantidad maxima de vacas a devolver", example = "10")
            @RequestParam(required = false)
            Integer limit
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return cowIncidentReportService.getCowIncidentRecurrenceReport(filter, limit);
    }
}
