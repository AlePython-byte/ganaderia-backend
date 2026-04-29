package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.UserCreateRequestDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.service.UserService;
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
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Administracion de usuarios internos del backend")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Crear usuario",
            description = "Registra un usuario interno del backend. Es una operacion administrativa y requiere rol ADMINISTRADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario creado correctamente",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese correo",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public UserResponseDTO createUser(@Valid @RequestBody UserCreateRequestDTO requestDTO) {
        return userService.createUser(requestDTO);
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar todos los usuarios (legacy)",
            description = "Endpoint legacy sin paginacion. Se mantiene por compatibilidad y se recomienda usar /api/users/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado completo de usuarios",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/page")
    @Operation(
            summary = "Listar usuarios paginados",
            description = "Consulta paginada de usuarios con filtro opcional por estado activo y ordenamiento controlado por el backend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de usuarios obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacion o filtro invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<UserResponseDTO> getUsersPage(
            @Parameter(description = "Filtro opcional por estado activo del usuario")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Numero de pagina base cero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "20")
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @Parameter(description = "Campo de ordenamiento permitido. Ejemplos: id, name, email, role, active", example = "id")
            @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direccion de ordenamiento", example = "ASC")
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        return userService.getUsersPage(active, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener usuario por id",
            description = "Recupera un usuario interno por su identificador numerico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public UserResponseDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/active/{active}")
    @Deprecated(since = "2.5", forRemoval = false)
    @Operation(
            summary = "Listar usuarios por estado activo (legacy)",
            description = "Endpoint legacy sin paginacion para filtrar usuarios activos o inactivos. Se mantiene por compatibilidad y se recomienda usar /api/users/page.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado filtrado por estado activo",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public List<UserResponseDTO> getUsersByActive(
            @Parameter(description = "Estado activo exacto que se desea consultar")
            @PathVariable Boolean active) {
        return userService.getUsersByActive(active);
    }
}
