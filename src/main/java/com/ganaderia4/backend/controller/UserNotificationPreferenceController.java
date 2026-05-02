package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.UserNotificationPreferenceRequestDTO;
import com.ganaderia4.backend.dto.UserNotificationPreferenceResponseDTO;
import com.ganaderia4.backend.service.UserNotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/notification-preferences")
@Tag(name = "Preferencias de notificacion", description = "Gestion administrativa de preferencias basicas de notificacion por usuario")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class UserNotificationPreferenceController {

    private final UserNotificationPreferenceService preferenceService;

    public UserNotificationPreferenceController(UserNotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener preferencias de notificacion de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias obtenidas correctamente",
                    content = @Content(schema = @Schema(implementation = UserNotificationPreferenceResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public UserNotificationPreferenceResponseDTO getPreferences(@PathVariable Long userId) {
        return preferenceService.getByUserId(userId);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear o actualizar preferencias de notificacion de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias actualizadas correctamente",
                    content = @Content(schema = @Schema(implementation = UserNotificationPreferenceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public UserNotificationPreferenceResponseDTO upsertPreferences(@PathVariable Long userId,
                                                                   @Valid @RequestBody UserNotificationPreferenceRequestDTO requestDTO) {
        return preferenceService.upsert(userId, requestDTO);
    }
}
