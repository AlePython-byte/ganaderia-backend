package com.ganaderia4.backend.pattern.observer.geofence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogGeofenceExitObserver implements GeofenceExitObserver {

    private static final Logger logger = LoggerFactory.getLogger(LogGeofenceExitObserver.class);

    @Override
    public void onGeofenceExit(GeofenceExitEvent event) {
        logger.warn(
                "Evento de salida de geocerca: vaca={}, locationId={}, lat={}, lon={}",
                event.getCow().getToken(),
                event.getLocation().getId(),
                event.getLocation().getLatitude(),
                event.getLocation().getLongitude()
        );
    }
}