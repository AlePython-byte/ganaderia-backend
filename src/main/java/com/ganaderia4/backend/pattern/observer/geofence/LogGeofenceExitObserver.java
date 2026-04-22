package com.ganaderia4.backend.pattern.observer.geofence;

import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogGeofenceExitObserver implements GeofenceExitObserver {

    private static final Logger logger = LoggerFactory.getLogger(LogGeofenceExitObserver.class);

    @Override
    public void onGeofenceExit(GeofenceExitEvent event) {
        logger.warn(
                "event=geofence_exit_detected cow={} locationId={}",
                OperationalLogSanitizer.maskToken(event.getCow().getToken()),
                event.getLocation().getId()
        );
    }
}
