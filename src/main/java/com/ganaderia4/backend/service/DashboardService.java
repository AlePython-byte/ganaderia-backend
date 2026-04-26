package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.*;
import com.ganaderia4.backend.model.*;
import com.ganaderia4.backend.pattern.builder.LocationResponseDTOBuilder;
import com.ganaderia4.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AlertRepository alertRepository;
    private final CowRepository cowRepository;
    private final CollarRepository collarRepository;
    private final LocationRepository locationRepository;
    private final AlertService alertService;

    public DashboardService(AlertRepository alertRepository,
                            CowRepository cowRepository,
                            CollarRepository collarRepository,
                            LocationRepository locationRepository,
                            AlertService alertService) {
        this.alertRepository = alertRepository;
        this.cowRepository = cowRepository;
        this.collarRepository = collarRepository;
        this.locationRepository = locationRepository;
        this.alertService = alertService;
    }

    public DashboardSummaryDTO getSummary() {
        List<Location> recentLocations = locationRepository.findTop10ByOrderByTimestampDesc();
        LocalDateTime latestLocationTimestamp = recentLocations.isEmpty()
                ? null
                : recentLocations.get(0).getTimestamp();

        return new DashboardSummaryDTO(
                cowRepository.count(),
                cowRepository.countByStatus(CowStatus.FUERA),
                collarRepository.count(),
                collarRepository.countByStatus(CollarStatus.ACTIVO),
                collarRepository.countByEnabledTrueAndSignalStatus(DeviceSignalStatus.SIN_SENAL),
                alertRepository.countByStatus(AlertStatus.PENDIENTE),
                alertRepository.countByTypeAndStatus(AlertType.EXIT_GEOFENCE, AlertStatus.PENDIENTE),
                alertRepository.countByTypeAndStatus(AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE),
                latestLocationTimestamp
        );
    }

    public List<AlertResponseDTO> getCriticalAlerts() {
        return getPrioritizedAlertQueue(10);
    }

    public List<AlertResponseDTO> getPrioritizedAlertQueue(Integer limit) {
        return alertService.getPendingAlertPriorityQueue(limit);
    }

    public List<CollarResponseDTO> getOfflineCollars() {
        return collarRepository.findByEnabledTrueAndSignalStatus(DeviceSignalStatus.SIN_SENAL)
                .stream()
                .map(this::mapCollarToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<CowResponseDTO> getCowsOutsideGeofence() {
        return cowRepository.findByStatus(CowStatus.FUERA)
                .stream()
                .map(this::mapCowToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getRecentLocations() {
        return locationRepository.findTop10ByOrderByTimestampDesc()
                .stream()
                .map(this::mapLocationToResponseDTO)
                .collect(Collectors.toList());
    }

    private CollarResponseDTO mapCollarToResponseDTO(Collar collar) {
        Long cowId = null;
        String cowToken = null;
        String cowName = null;

        if (collar.getCow() != null) {
            cowId = collar.getCow().getId();
            cowToken = collar.getCow().getToken();
            cowName = collar.getCow().getName();
        }

        return new CollarResponseDTO(
                collar.getId(),
                collar.getToken(),
                collar.getStatus().name(),
                cowId,
                cowToken,
                cowName,
                collar.getBatteryLevel(),
                collar.getLastSeenAt(),
                collar.getSignalStatus() != null ? collar.getSignalStatus().name() : null,
                collar.getFirmwareVersion(),
                collar.getGpsAccuracy(),
                collar.getEnabled(),
                collar.getNotes()
        );
    }

    private CowResponseDTO mapCowToResponseDTO(Cow cow) {
        return new CowResponseDTO(
                cow.getId(),
                cow.getToken(),
                cow.getInternalCode(),
                cow.getName(),
                cow.getStatus().name(),
                cow.getObservations()
        );
    }

    private LocationResponseDTO mapLocationToResponseDTO(Location location) {
        return new LocationResponseDTOBuilder()
                .id(location.getId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .timestamp(location.getTimestamp())
                .cowId(location.getCow().getId())
                .cowToken(location.getCow().getToken())
                .cowName(location.getCow().getName())
                .collarToken(location.getCollar() != null ? location.getCollar().getToken() : null)
                .build();
    }
}
