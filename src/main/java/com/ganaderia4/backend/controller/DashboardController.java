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

    @GetMapping("/prioritized-alert-queue")
    public List<AlertResponseDTO> getPrioritizedAlertQueue(
            @RequestParam(required = false) Integer limit
    ) {
        return dashboardService.getPrioritizedAlertQueue(limit);
    }

    @GetMapping("/pending-alert-aging")
    public PendingAlertAgingDTO getPendingAlertAging() {
        return dashboardService.getPendingAlertAging();
    }

    @GetMapping("/telemetry-freshness")
    public TelemetryFreshnessDTO getTelemetryFreshness() {
        return dashboardService.getTelemetryFreshness();
    }

    @GetMapping("/top-problematic-cows")
    public List<CowIncidentReportDTO> getTopProblematicCows(
            @RequestParam(required = false) Integer limit
    ) {
        return dashboardService.getTopProblematicCows(limit);
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
