package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.GeofenceRequestDTO;
import com.ganaderia4.backend.dto.GeofenceResponseDTO;
import com.ganaderia4.backend.service.GeofenceService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@Tag(name = "Geocercas", description = "Gestion de geocercas operativas asociadas a vacas")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @PostMapping
    @Operation(
            summary = "Crear geocerca",
            description = "Registra una geocerca nueva. Si se marca activa y se informa una vaca, el backend valida que esa vaca no tenga otra geocerca activa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geocerca creada correctamente",
                    content = @Content(schema = @Schema(implementation = GeofenceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca asociada no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "La vaca ya tiene una geocerca activa asignada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public GeofenceResponseDTO createGeofence(@Valid @RequestBody GeofenceRequestDTO requestDTO) {
        return geofenceService.createGeofence(requestDTO);
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar todas las geocercas (legacy)",
            description = "Endpoint legacy sin paginacion. Se mantiene por compatibilidad y se recomienda usar /api/geofences/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de geocercas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GeofenceResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<GeofenceResponseDTO> getAllGeofences() {
        return geofenceService.getAllGeofences();
    }

    @GetMapping("/page")
    @Operation(
            summary = "Listar geocercas paginadas",
            description = "Consulta paginada de geocercas con filtro opcional por estado activo y ordenamiento controlado por el backend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de geocercas obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacion o filtro invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<GeofenceResponseDTO> getGeofencesPage(
            @Parameter(description = "Filtro opcional por estado activo de la geocerca")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @Parameter(description = "Campo de ordenamiento permitido. Ejemplos: id, name, active, radiusMeters, centerLatitude, centerLongitude", example = "id")
            @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direccion de ordenamiento", example = "ASC")
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return geofenceService.getGeofencesPage(active, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener geocerca por id",
            description = "Recupera una geocerca registrada por su identificador numerico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geocerca encontrada",
                    content = @Content(schema = @Schema(implementation = GeofenceResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Geocerca no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public GeofenceResponseDTO getGeofenceById(@PathVariable Long id) {
        return geofenceService.getGeofenceById(id);
    }

    @GetMapping("/active/{active}")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar geocercas por estado activo (legacy)",
            description = "Endpoint legacy sin paginacion para filtrar geocercas activas o inactivas. Se mantiene por compatibilidad y se recomienda usar /api/geofences/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado filtrado por estado activo",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GeofenceResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<GeofenceResponseDTO> getGeofencesByActive(
            @Parameter(description = "Estado activo exacto que se desea consultar")
            @PathVariable Boolean active) {
        return geofenceService.getGeofencesByActive(active);
    }
}
