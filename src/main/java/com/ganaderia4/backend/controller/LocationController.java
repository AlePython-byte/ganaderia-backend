package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Ubicaciones", description = "Registro manual y consultas historicas de ubicaciones")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @Operation(
            summary = "Registrar ubicacion manual",
            description = "Registra una ubicacion manual o desde API autenticada. El contrato temporal actual interpreta los LocalDateTime como UTC segun la politica temporal del proyecto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ubicacion registrada correctamente",
                    content = @Content(schema = @Schema(implementation = LocationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido o datos fuera de rango",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca o collar no encontrados cuando aplica",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public LocationResponseDTO registerLocation(@Valid @RequestBody LocationRequestDTO requestDTO) {
        return locationService.registerLocation(requestDTO);
    }

    @GetMapping("/cow/{cowId}")
    @Operation(
            summary = "Obtener historial de ubicaciones por vaca",
            description = "Devuelve el historial paginado de ubicaciones de una vaca ordenado por timestamp descendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacion invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<LocationResponseDTO> getLocationHistoryByCow(
            @Parameter(description = "Identificador numerico de la vaca")
            @PathVariable Long cowId,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size) {
        return locationService.getLocationHistoryByCow(cowId, page, size);
    }

    @GetMapping("/cow/{cowId}/between")
    @Operation(
            summary = "Obtener historial por vaca en rango temporal",
            description = "Devuelve el historial paginado de ubicaciones de una vaca dentro de un rango temporal. Los parametros start y end usan LocalDateTime ISO y deben interpretarse como UTC segun la politica temporal vigente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial filtrado obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "Rango temporal o paginacion invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<LocationResponseDTO> getLocationHistoryByCowAndDates(
            @Parameter(description = "Identificador numerico de la vaca")
            @PathVariable Long cowId,
            @Parameter(description = "Inicio del rango temporal en formato ISO local date-time interpretado como UTC", example = "2026-04-28T20:52:08")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "Fin del rango temporal en formato ISO local date-time interpretado como UTC", example = "2026-04-28T21:52:08")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size) {
        return locationService.getLocationHistoryByCowAndDates(cowId, start, end, page, size);
    }

    @GetMapping("/cow/{cowId}/last")
    @Operation(
            summary = "Obtener ultima ubicacion por vaca",
            description = "Recupera la ubicacion mas reciente registrada para una vaca."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ultima ubicacion obtenida correctamente",
                    content = @Content(schema = @Schema(implementation = LocationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca no encontrada o sin ubicaciones registradas",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public LocationResponseDTO getLastLocationByCow(
            @Parameter(description = "Identificador numerico de la vaca")
            @PathVariable Long cowId) {
        return locationService.getLastLocationByCow(cowId);
    }
}
