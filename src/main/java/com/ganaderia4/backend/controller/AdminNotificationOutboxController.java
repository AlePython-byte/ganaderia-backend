package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.NotificationOutboxDetailDTO;
import com.ganaderia4.backend.dto.NotificationOutboxSummaryDTO;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.service.NotificationOutboxQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notification-outbox")
@Tag(name = "Notification outbox admin", description = "Diagnostico administrativo de solo lectura del notification outbox")
@SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
public class AdminNotificationOutboxController {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationOutboxController.class);

    private final NotificationOutboxQueryService notificationOutboxQueryService;

    public AdminNotificationOutboxController(NotificationOutboxQueryService notificationOutboxQueryService) {
        this.notificationOutboxQueryService = notificationOutboxQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar mensajes del notification outbox")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensajes obtenidos correctamente"),
            @ApiResponse(responseCode = "400", description = "Filtro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Page<NotificationOutboxSummaryDTO> list(
            @Parameter(description = "Estado opcional", example = "FAILED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Canal opcional", example = "EMAIL")
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info(
                "event=notification_outbox_admin_list requestId={} status={} channel={} page={} size={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(status),
                OperationalLogSanitizer.safe(channel),
                page,
                size
        );
        return notificationOutboxQueryService.list(status, channel, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener detalle seguro de un mensaje del notification outbox")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public NotificationOutboxDetailDTO getById(@PathVariable Long id) {
        log.info(
                "event=notification_outbox_admin_detail requestId={} messageId={}",
                OperationalLogSanitizer.requestId(),
                id
        );
        return notificationOutboxQueryService.getById(id);
    }
}
