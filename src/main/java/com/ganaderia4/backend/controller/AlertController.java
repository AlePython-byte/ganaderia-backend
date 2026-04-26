package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertUpdateRequestDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Deprecated(since = "2.5", forRemoval = false)
    public List<AlertResponseDTO> getAllAlerts() {
        return alertService.getAllAlertsLegacy();
    }

    @GetMapping("/page")
    public Page<AlertResponseDTO> getAlertsPage(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.pagination.default-size:20}") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        return alertService.getAlertsPage(status, type, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public AlertResponseDTO getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id);
    }

    @GetMapping("/status/{status}")
    @Deprecated(since = "2.5", forRemoval = false)
    public List<AlertResponseDTO> getAlertsByStatus(@PathVariable AlertStatus status) {
        return alertService.getAlertsByStatusLegacy(status);
    }

    @GetMapping("/pending/priority-queue")
    public List<AlertResponseDTO> getPendingAlertPriorityQueue(
            @RequestParam(required = false) Integer limit
    ) {
        return alertService.getPendingAlertPriorityQueue(limit);
    }

    @GetMapping("/type/{type}")
    @Deprecated(since = "2.5", forRemoval = false)
    public List<AlertResponseDTO> getAlertsByType(@PathVariable AlertType type) {
        return alertService.getAlertsByTypeLegacy(type);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlertResponseDTO updateAlert(@PathVariable Long id,
                                        @Valid @RequestBody AlertUpdateRequestDTO requestDTO) {
        return alertService.updateAlert(id, requestDTO);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlertResponseDTO resolveAlert(@PathVariable Long id,
                                         @RequestParam(required = false) String observations) {
        return alertService.resolveAlert(id, observations);
    }

    @PatchMapping("/{id}/discard")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlertResponseDTO discardAlert(@PathVariable Long id,
                                         @RequestParam(required = false) String observations) {
        return alertService.discardAlert(id, observations);
    }
}
