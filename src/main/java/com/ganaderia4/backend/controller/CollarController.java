package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.CollarRequestDTO;
import com.ganaderia4.backend.dto.CollarResponseDTO;
import com.ganaderia4.backend.dto.DeviceSecretResponseDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.service.CollarService;
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
@RequestMapping("/api/collars")
@Tag(name = "Collares", description = "Gestion operativa de collares, asignacion a vacas y administracion de credenciales HMAC")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class CollarController {

    private final CollarService collarService;

    public CollarController(CollarService collarService) {
        this.collarService = collarService;
    }

    @PostMapping
    @Operation(
            summary = "Crear collar",
            description = "Registra un collar nuevo. Puede asociarse a una vaca durante la creacion si se informa cowId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar creado correctamente",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca asociada no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto por token duplicado o vaca ya asociada a otro collar",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO createCollar(@Valid @RequestBody CollarRequestDTO requestDTO) {
        return collarService.createCollar(requestDTO);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar collar",
            description = "Actualiza un collar existente. El token publico del collar debe mantenerse estable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido o intento de modificar el token estable",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar o vaca no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de asignacion con otra vaca o collar",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO updateCollar(@PathVariable Long id,
                                          @Valid @RequestBody CollarRequestDTO requestDTO) {
        return collarService.updateCollar(id, requestDTO);
    }

    @PatchMapping("/{id}/enable")
    @Operation(
            summary = "Habilitar collar",
            description = "Marca el collar como habilitado para que vuelva a aceptar procesamiento operativo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar habilitado o ya habilitado",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO enableCollar(@PathVariable Long id) {
        return collarService.enableCollar(id);
    }

    @PatchMapping("/{id}/disable")
    @Operation(
            summary = "Deshabilitar collar",
            description = "Marca el collar como deshabilitado para detener su procesamiento operativo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar deshabilitado o ya deshabilitado",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO disableCollar(@PathVariable Long id) {
        return collarService.disableCollar(id);
    }

    @PatchMapping("/{id}/rotate-secret")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Rotar secreto HMAC del collar",
            description = "Genera un nuevo secreto de firma para el collar. Requiere rol ADMINISTRADOR y debe coordinarse con el dispositivo antes de volver a enviar telemetria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secreto HMAC rotado correctamente",
                    content = @Content(schema = @Schema(implementation = DeviceSecretResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public DeviceSecretResponseDTO rotateDeviceSecret(@PathVariable Long id) {
        return collarService.rotateDeviceSecret(id);
    }

    @PatchMapping("/{id}/assign/{cowId}")
    @Operation(
            summary = "Reasignar collar a una vaca",
            description = "Asocia un collar existente a una vaca destino. Si la vaca ya tiene otro collar, la operacion falla por conflicto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar reasignado correctamente",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar o vaca destino no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "La vaca destino ya tiene otro collar asociado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO reassignCollar(@PathVariable Long id,
                                            @PathVariable Long cowId) {
        return collarService.reassignCollar(id, cowId);
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar todos los collares (legacy)",
            description = "Endpoint legacy sin paginacion. Se mantiene por compatibilidad y se recomienda usar /api/collars/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de collares",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollarResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CollarResponseDTO> getAllCollars() {
        return collarService.getAllCollars();
    }

    @GetMapping("/page")
    @Operation(
            summary = "Listar collares paginados",
            description = "Consulta paginada de collares con filtros por estado y ordenamiento controlado por el backend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de collares obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacion o filtro invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<CollarResponseDTO> getCollarsPage(
            @Parameter(description = "Filtro opcional por estado del collar")
            @RequestParam(required = false) CollarStatus status,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @Parameter(description = "Campo de ordenamiento permitido. Ejemplos: id, token, status, batteryLevel, lastSeenAt, signalStatus, enabled", example = "id")
            @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direccion de ordenamiento", example = "ASC")
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return collarService.getCollarsPage(status, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener collar por id",
            description = "Recupera un collar registrado por su identificador numerico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar encontrado",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO getCollarById(@PathVariable Long id) {
        return collarService.getCollarById(id);
    }

    @GetMapping("/status/{status}")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar collares por estado (legacy)",
            description = "Endpoint legacy sin paginacion para filtrar por estado. Se mantiene por compatibilidad y se recomienda usar /api/collars/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado filtrado por estado",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollarResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CollarResponseDTO> getCollarsByStatus(
            @Parameter(description = "Estado exacto del collar que se desea consultar")
            @PathVariable CollarStatus status) {
        return collarService.getCollarsByStatus(status);
    }

    @GetMapping("/token/{token}")
    @Operation(
            summary = "Obtener collar por token",
            description = "Recupera un collar por su token publico estable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collar encontrado",
                    content = @Content(schema = @Schema(implementation = CollarResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Collar no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CollarResponseDTO getCollarByToken(
            @Parameter(description = "Token publico estable del collar", example = "COL-001")
            @PathVariable String token) {
        return collarService.getCollarByToken(token);
    }
}
