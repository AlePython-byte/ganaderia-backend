package com.ganaderia4.backend.pattern.factory.alert;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.builder.AlertBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ExitGeofenceAlertCreator implements AlertCreator {

    @Override
    public AlertType getSupportedType() {
        return AlertType.EXIT_GEOFENCE;
    }

    @Override
    public Alert create(Cow cow, Location location) {
        return new AlertBuilder()
                .type(AlertType.EXIT_GEOFENCE)
                .message("La vaca " + cow.getToken() + " salió de la geocerca activa")
                .createdAt(LocalDateTime.now())
                .status(AlertStatus.PENDIENTE)
                .observations("Alerta generada automáticamente por salida de geocerca")
                .cow(cow)
                .location(location)
                .build();
    }
}