package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Geofence;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final CollarRepository collarRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeofenceService geofenceService;
    private final AlertService alertService;

    public LocationService(LocationRepository locationRepository,
                           CollarRepository collarRepository,
                           GeofenceRepository geofenceRepository,
                           GeofenceService geofenceService,
                           AlertService alertService) {
        this.locationRepository = locationRepository;
        this.collarRepository = collarRepository;
        this.geofenceRepository = geofenceRepository;
        this.geofenceService = geofenceService;
        this.alertService = alertService;
    }

    public LocationResponseDTO registerLocation(LocationRequestDTO requestDTO) {
        validateCoordinates(requestDTO.getLatitude(), requestDTO.getLongitude());

        Collar collar = collarRepository.findByIdentifier(requestDTO.getCollarIdentifier())
                .orElseThrow(() -> new RuntimeException("Collar no registrado"));

        if (collar.getCow() == null) {
            throw new RuntimeException("El collar no está asociado a ninguna vaca");
        }

        Cow cow = collar.getCow();

        Location location = new Location();
        location.setLatitude(requestDTO.getLatitude());
        location.setLongitude(requestDTO.getLongitude());
        location.setTimestamp(requestDTO.getTimestamp());
        location.setCow(cow);

        Location savedLocation = locationRepository.save(location);

        geofenceRepository.findByCowAndActive(cow, true).ifPresent(geofence -> {
            boolean inside = geofenceService.isInsideGeofence(
                    savedLocation.getLatitude(),
                    savedLocation.getLongitude(),
                    geofence
            );

            if (!inside) {
                alertService.createExitGeofenceAlert(cow, savedLocation);
                cow.setStatus("FUERA");
            } else {
                cow.setStatus("DENTRO");
            }
        });

        return mapToResponseDTO(savedLocation, collar.getIdentifier());
    }

    public List<LocationResponseDTO> getLocationHistoryByCow(Long cowId) {
        Cow cow = new Cow();
        cow.setId(cowId);

        return locationRepository.findByCowOrderByTimestampAsc(cow)
                .stream()
                .map(location -> mapToResponseDTO(location, null))
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getLocationHistoryByCowAndDates(Long cowId, LocalDateTime start, LocalDateTime end) {
        Cow cow = new Cow();
        cow.setId(cowId);

        return locationRepository.findByCowAndTimestampBetweenOrderByTimestampAsc(cow, start, end)
                .stream()
                .map(location -> mapToResponseDTO(location, null))
                .collect(Collectors.toList());
    }

    public LocationResponseDTO getLastLocationByCow(Long cowId) {
        Cow cow = new Cow();
        cow.setId(cowId);

        Location location = locationRepository.findTopByCowOrderByTimestampDesc(cow)
                .orElseThrow(() -> new RuntimeException("No existe ubicación registrada para esa vaca"));

        return mapToResponseDTO(location, null);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new RuntimeException("Latitud no válida");
        }

        if (longitude < -180 || longitude > 180) {
            throw new RuntimeException("Longitud no válida");
        }
    }

    private LocationResponseDTO mapToResponseDTO(Location location, String collarIdentifier) {
        return new LocationResponseDTO(
                location.getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getTimestamp(),
                location.getCow().getId(),
                location.getCow().getIdentifier(),
                location.getCow().getName(),
                collarIdentifier
        );
    }
}