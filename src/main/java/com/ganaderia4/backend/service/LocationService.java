package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeofenceService geofenceService;
    private final AlertService alertService;

    public LocationService(LocationRepository locationRepository,
                           CollarRepository collarRepository,
                           CowRepository cowRepository,
                           GeofenceRepository geofenceRepository,
                           GeofenceService geofenceService,
                           AlertService alertService) {
        this.locationRepository = locationRepository;
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
        this.geofenceRepository = geofenceRepository;
        this.geofenceService = geofenceService;
        this.alertService = alertService;
    }

    @Transactional
    public LocationResponseDTO registerLocation(LocationRequestDTO requestDTO) {
        validateCoordinates(requestDTO.getLatitude(), requestDTO.getLongitude());

        Collar collar = collarRepository.findByIdentifier(requestDTO.getCollarIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Collar no registrado"));

        if (collar.getCow() == null) {
            throw new BadRequestException("El collar no está asociado a ninguna vaca");
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
                cow.setStatus(CowStatus.FUERA);
            } else {
                cow.setStatus(CowStatus.DENTRO);
            }

            cowRepository.save(cow);
        });

        return mapToResponseDTO(savedLocation, collar.getIdentifier());
    }

    public List<LocationResponseDTO> getLocationHistoryByCow(Long cowId) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        return locationRepository.findByCowOrderByTimestampAsc(cow)
                .stream()
                .map(location -> mapToResponseDTO(location, null))
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getLocationHistoryByCowAndDates(Long cowId, LocalDateTime start, LocalDateTime end) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        return locationRepository.findByCowAndTimestampBetweenOrderByTimestampAsc(cow, start, end)
                .stream()
                .map(location -> mapToResponseDTO(location, null))
                .collect(Collectors.toList());
    }

    public LocationResponseDTO getLastLocationByCow(Long cowId) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        Location location = locationRepository.findTopByCowOrderByTimestampDesc(cow)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ubicación registrada para esa vaca"));

        return mapToResponseDTO(location, null);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("Latitud no válida");
        }

        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("Longitud no válida");
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