package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.CowRequestDTO;
import com.ganaderia4.backend.dto.CowResponseDTO;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.service.CowService;
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
@RequestMapping("/api/cows")
@Tag(name = "Vacas", description = "Gestion operativa de vacas y consultas de lectura del dominio ganadero")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class CowController {

    private final CowService cowService;

    public CowController(CowService cowService) {
        this.cowService = cowService;
    }

    @PostMapping
    @Operation(
            summary = "Crear vaca",
            description = "Registra una nueva vaca en el sistema. El token publico es generado automaticamente por el backend; si el cliente lo envia, se ignora por compatibilidad. El codigo interno no puede repetirse cuando se informa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vaca creada correctamente",
                    content = @Content(schema = @Schema(implementation = CowResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto por codigo interno duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CowResponseDTO createCow(@Valid @RequestBody CowRequestDTO requestDTO) {
        return cowService.createCow(requestDTO);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar vaca",
            description = "Actualiza los datos editables de una vaca existente identificada por id. Si no se informa token, el backend conserva el valor actual; si se informa, debe seguir siendo unico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vaca actualizada correctamente",
                    content = @Content(schema = @Schema(implementation = CowResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto por token o codigo interno duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CowResponseDTO updateCow(@PathVariable Long id,
                                    @Valid @RequestBody CowRequestDTO requestDTO) {
        return cowService.updateCow(id, requestDTO);
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar todas las vacas (legacy)",
            description = "Endpoint legacy sin paginacion. Se mantiene por compatibilidad y se recomienda usar /api/cows/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de vacas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CowResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CowResponseDTO> getAllCows() {
        return cowService.getAllCows();
    }

    @GetMapping("/page")
    @Operation(
            summary = "Listar vacas paginadas",
            description = "Consulta paginada de vacas con filtros por estado y ordenamiento controlado por el backend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de vacas obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacion o filtro invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<CowResponseDTO> getCowsPage(
            @Parameter(description = "Filtro opcional por estado de la vaca")
            @RequestParam(required = false) CowStatus status,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @Parameter(description = "Campo de ordenamiento permitido. Ejemplos: id, token, internalCode, name, status", example = "id")
            @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direccion de ordenamiento", example = "ASC")
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return cowService.getCowsPage(status, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener vaca por id",
            description = "Recupera una vaca registrada por su identificador numerico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vaca encontrada",
                    content = @Content(schema = @Schema(implementation = CowResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CowResponseDTO getCowById(@PathVariable Long id) {
        return cowService.getCowById(id);
    }

    @GetMapping("/status/{status}")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar vacas por estado (legacy)",
            description = "Endpoint legacy sin paginacion para filtrar por estado. Se mantiene por compatibilidad y se recomienda usar /api/cows/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado filtrado por estado",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CowResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<CowResponseDTO> getCowsByStatus(
            @Parameter(description = "Estado exacto que se desea consultar")
            @PathVariable CowStatus status) {
        return cowService.getCowsByStatus(status);
    }

    @GetMapping("/token/{token}")
    @Operation(
            summary = "Obtener vaca por token",
            description = "Recupera una vaca por su token publico estable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vaca encontrada",
                    content = @Content(schema = @Schema(implementation = CowResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaca no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public CowResponseDTO getCowByToken(
            @Parameter(description = "Token publico estable de la vaca", example = "COW-001")
            @PathVariable String token) {
        return cowService.getCowByToken(token);
    }
}
