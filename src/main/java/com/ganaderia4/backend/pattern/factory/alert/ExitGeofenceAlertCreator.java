package com.ganaderia4.backend.pattern.factory.alert;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
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
        Alert alert = new Alert();
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setMessage("La vaca " + cow.getToken() + " salió de la geocerca activa");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setObservations("Alerta generada automáticamente por salida de geocerca");
        alert.setCow(cow);
        alert.setLocation(location);
        return alert;
    }
}