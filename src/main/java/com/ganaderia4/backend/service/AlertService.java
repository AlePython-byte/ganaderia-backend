package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Alert createExitGeofenceAlert(Cow cow, Location location) {
        if (alertRepository.findByCowAndTypeAndStatus(cow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE).isPresent()) {
            return null;
        }

        Alert alert = new Alert();
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setMessage("La vaca " + cow.getIdentifier() + " salió de la geocerca activa");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setObservations("Alerta generada automáticamente por salida de geocerca");
        alert.setCow(cow);
        alert.setLocation(location);

        return alertRepository.save(alert);
    }

    public List<AlertResponseDTO> getAllAlerts() {
        return alertRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AlertResponseDTO> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AlertResponseDTO> getAlertsByType(AlertType type) {
        return alertRepository.findByType(type)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public AlertResponseDTO getAlertById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada"));

        return mapToResponseDTO(alert);
    }

    private AlertResponseDTO mapToResponseDTO(Alert alert) {
        Long locationId = null;
        if (alert.getLocation() != null) {
            locationId = alert.getLocation().getId();
        }

        return new AlertResponseDTO(
                alert.getId(),
                alert.getType().name(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getStatus().name(),
                alert.getObservations(),
                alert.getCow().getId(),
                alert.getCow().getIdentifier(),
                alert.getCow().getName(),
                locationId
        );
    }
}