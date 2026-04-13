package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.*;
import com.ganaderia4.backend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/critical-alerts")
    public List<AlertResponseDTO> getCriticalAlerts() {
        return dashboardService.getCriticalAlerts();
    }

    @GetMapping("/collars-offline")
    public List<CollarResponseDTO> getOfflineCollars() {
        return dashboardService.getOfflineCollars();
    }

    @GetMapping("/cows-outside-geofence")
    public List<CowResponseDTO> getCowsOutsideGeofence() {
        return dashboardService.getCowsOutsideGeofence();
    }

    @GetMapping("/recent-locations")
    public List<LocationResponseDTO> getRecentLocations() {
        return dashboardService.getRecentLocations();
    }
}