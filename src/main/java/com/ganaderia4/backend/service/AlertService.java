package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertUpdateRequestDTO;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.factory.alert.AlertFactory;
import com.ganaderia4.backend.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertFactory alertFactory;
    private final AuditLogService auditLogService;

    public AlertService(AlertRepository alertRepository,
                        AlertFactory alertFactory,
                        AuditLogService auditLogService) {
        this.alertRepository = alertRepository;
        this.alertFactory = alertFactory;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Alert createExitGeofenceAlert(Cow cow, Location location) {
        if (alertRepository.findByCowAndTypeAndStatus(cow, AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE).isPresent()) {
            return null;
        }

        Alert alert = alertFactory.createAlert(AlertType.EXIT_GEOFENCE, cow, location);
        Alert savedAlert = alertRepository.save(alert);

        auditLogService.log(
                "CREATE_EXIT_GEOFENCE_ALERT",
                "ALERT",
                savedAlert.getId(),
                "SYSTEM",
                "SYSTEM",
                "Alerta automática por salida de geocerca para vaca " + cow.getToken(),
                true
        );

        return savedAlert;
    }

    @Transactional
    public Alert createCollarOfflineAlert(Collar collar) {
        if (collar == null || collar.getCow() == null) {
            return null;
        }

        Cow cow = collar.getCow();

        if (alertRepository.findByCowAndTypeAndStatus(cow, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE).isPresent()) {
            return null;
        }

        Alert alert = new Alert();
        alert.setType(AlertType.COLLAR_OFFLINE);
        alert.setCow(cow);
        alert.setLocation(null);
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDIENTE);

        String lastSeenText = collar.getLastSeenAt() != null
                ? collar.getLastSeenAt().toString()
                : "sin registros previos";

        alert.setMessage("El collar " + collar.getToken() + " no ha reportado ubicación recientemente. Último reporte: " + lastSeenText);
        alert.setObservations("Alerta operativa generada automáticamente por falta de reporte del dispositivo");

        Alert savedAlert = alertRepository.save(alert);

        auditLogService.log(
                "CREATE_COLLAR_OFFLINE_ALERT",
                "ALERT",
                savedAlert.getId(),
                "SYSTEM",
                "SYSTEM",
                "Alerta automática por collar offline " + collar.getToken(),
                true
        );

        return savedAlert;
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

        auditLogService.logWithCurrentActor(
                "UPDATE_ALERT",
                "ALERT",
                updatedAlert.getId(),
                "API",
                "Actualización de alerta " + updatedAlert.getId(),
                true
        );

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

        auditLogService.logWithCurrentActor(
                "RESOLVE_ALERT",
                "ALERT",
                updatedAlert.getId(),
                "API",
                "Resolución manual de alerta " + updatedAlert.getId(),
                true
        );

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

        auditLogService.logWithCurrentActor(
                "DISCARD_ALERT",
                "ALERT",
                updatedAlert.getId(),
                "API",
                "Descarte manual de alerta " + updatedAlert.getId(),
                true
        );

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