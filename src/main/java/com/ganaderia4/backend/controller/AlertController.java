package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertUpdateRequestDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alertas", description = "Gestión de alertas del sistema")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Listar alertas")
    public List<AlertResponseDTO> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar alerta por id")
    public AlertResponseDTO getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar alertas por estado")
    public List<AlertResponseDTO> getAlertsByStatus(@PathVariable AlertStatus status) {
        return alertService.getAlertsByStatus(status);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Listar alertas por tipo")
    public List<AlertResponseDTO> getAlertsByType(@PathVariable AlertType type) {
        return alertService.getAlertsByType(type);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar alerta")
    public AlertResponseDTO updateAlert(@PathVariable Long id,
                                        @Valid @RequestBody AlertUpdateRequestDTO requestDTO) {
        return alertService.updateAlert(id, requestDTO);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Resolver alerta")
    public AlertResponseDTO resolveAlert(@PathVariable Long id,
                                         @RequestParam(required = false) String observations) {
        return alertService.resolveAlert(id, observations);
    }

    @PatchMapping("/{id}/discard")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Descartar alerta")
    public AlertResponseDTO discardAlert(@PathVariable Long id,
                                         @RequestParam(required = false) String observations) {
        return alertService.discardAlert(id, observations);
    }
}