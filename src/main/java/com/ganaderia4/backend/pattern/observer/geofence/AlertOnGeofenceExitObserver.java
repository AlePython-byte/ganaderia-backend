package com.ganaderia4.backend.pattern.observer.geofence;

import com.ganaderia4.backend.service.AlertService;
import org.springframework.stereotype.Component;

@Component
public class AlertOnGeofenceExitObserver implements GeofenceExitObserver {

    private final AlertService alertService;

    public AlertOnGeofenceExitObserver(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void onGeofenceExit(GeofenceExitEvent event) {
        alertService.createExitGeofenceAlert(event.getCow(), event.getLocation());
    }
}