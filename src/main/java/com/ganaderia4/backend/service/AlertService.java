package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Alert createExitGeofenceAlert(Cow cow, Location location) {
        if (alertRepository.findByCowAndTypeAndStatus(cow, "EXIT_GEOFENCE", "PENDIENTE").isPresent()) {
            return null;
        }

        Alert alert = new Alert();
        alert.setType("EXIT_GEOFENCE");
        alert.setMessage("La vaca " + cow.getIdentifier() + " salió de la geocerca activa");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus("PENDIENTE");
        alert.setObservations("Alerta generada automáticamente por salida de geocerca");
        alert.setCow(cow);
        alert.setLocation(location);

        return alertRepository.save(alert);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public List<Alert> getAlertsByType(String type) {
        return alertRepository.findByType(type);
    }
}