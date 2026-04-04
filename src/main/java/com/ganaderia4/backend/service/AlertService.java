package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertUpdateRequestDTO;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.factory.alert.AlertFactory;
import com.ganaderia4.backend.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertFactory alertFactory;

    public AlertService(AlertRepository alertRepository, AlertFactory alertFactory) {
        this.alertRepository = alertRepository;
        this.alertFactory = alertFactory;
    }

    @Transactional
    public Alert createExitGeofenceAlert(Cow cow, Location location) {
        if (alertRepository.findByCowAndTypeAndStatus(cow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE).isPresent()) {
            return null;
        }

        Alert alert = alertFactory.createAlert(AlertType.EXIT_GEOFENCE, cow, location);
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

    @Transactional
    public AlertResponseDTO updateAlert(Long id, AlertUpdateRequestDTO requestDTO) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada"));

        alert.setStatus(requestDTO.getStatus());
        alert.setObservations(requestDTO.getObservations());

        Alert updatedAlert = alertRepository.save(alert);
        return mapToResponseDTO(updatedAlert);
    }

    @Transactional
    public AlertResponseDTO resolveAlert(Long id, String observations) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada"));

        alert.setStatus(AlertStatus.RESUELTA);

        if (observations != null && !observations.isBlank()) {
            alert.setObservations(observations);
        } else if (alert.getObservations() == null || alert.getObservations().isBlank()) {
            alert.setObservations("Alerta resuelta manualmente");
        }

        Alert updatedAlert = alertRepository.save(alert);
        return mapToResponseDTO(updatedAlert);
    }

    @Transactional
    public AlertResponseDTO discardAlert(Long id, String observations) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada"));

        alert.setStatus(AlertStatus.DESCARTADA);

        if (observations != null && !observations.isBlank()) {
            alert.setObservations(observations);
        } else if (alert.getObservations() == null || alert.getObservations().isBlank()) {
            alert.setObservations("Alerta descartada manualmente");
        }

        Alert updatedAlert = alertRepository.save(alert);
        return mapToResponseDTO(updatedAlert);
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
                alert.getCow().getToken(),
                alert.getCow().getName(),
                locationId
        );
    }
}