package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertService;
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
    public List<AlertResponseDTO> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/{id}")
    public AlertResponseDTO getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id);
    }

    @GetMapping("/status/{status}")
    public List<AlertResponseDTO> getAlertsByStatus(@PathVariable AlertStatus status) {
        return alertService.getAlertsByStatus(status);
    }

    @GetMapping("/type/{type}")
    public List<AlertResponseDTO> getAlertsByType(@PathVariable AlertType type) {
        return alertService.getAlertsByType(type);
    }
}