package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.NotificationOutboxDetailDTO;
import com.ganaderia4.backend.dto.NotificationOutboxSummaryDTO;
import com.ganaderia4.backend.dto.PagedResponseDTO;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.security.OutboxAdminAbuseProtectionService;
import com.ganaderia4.backend.service.NotificationOutboxQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final OutboxAdminAbuseProtectionService outboxAdminAbuseProtectionService;
    private final ClientIpResolver clientIpResolver;

    public AdminNotificationOutboxController(NotificationOutboxQueryService notificationOutboxQueryService,
                                             OutboxAdminAbuseProtectionService outboxAdminAbuseProtectionService,
                                             ClientIpResolver clientIpResolver) {
        this.notificationOutboxQueryService = notificationOutboxQueryService;
        this.outboxAdminAbuseProtectionService = outboxAdminAbuseProtectionService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar mensajes del notification outbox")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensajes obtenidos correctamente",
                    content = @Content(schema = @Schema(implementation = PagedResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public PagedResponseDTO<NotificationOutboxSummaryDTO> list(
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
        Page<NotificationOutboxSummaryDTO> result = notificationOutboxQueryService.list(status, channel, page, size);
        return PagedResponseDTO.from(result);
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

    @PostMapping("/{id}/requeue")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Reencolar un mensaje EMAIL fallido del notification outbox")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensaje reencolado correctamente"),
            @ApiResponse(responseCode = "401", description = "JWT ausente o invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "El estado o canal actual no permite reencolar",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "429",
                    description = "Se excedio el limite de reintentos administrativos de requeue para el usuario, IP o mensaje actual",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public NotificationOutboxDetailDTO requeue(@PathVariable Long id,
                                               Authentication authentication,
                                               HttpServletRequest httpServletRequest) {
        outboxAdminAbuseProtectionService.recordRequeueRequest(
                authentication == null ? null : authentication.getName(),
                clientIpResolver.resolve(httpServletRequest),
                id
        );
        return notificationOutboxQueryService.requeue(id);
    }
}
