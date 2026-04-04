package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.adapter.location.ApiLocationRequestAdapter;
import com.ganaderia4.backend.pattern.adapter.location.DeviceLocationPayloadAdapter;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.builder.LocationResponseDTOBuilder;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeofenceService geofenceService;
    private final AlertService alertService;
    private final ApiLocationRequestAdapter apiLocationRequestAdapter;
    private final DeviceLocationPayloadAdapter deviceLocationPayloadAdapter;

    public LocationService(LocationRepository locationRepository,
                           CollarRepository collarRepository,
                           CowRepository cowRepository,
                           GeofenceRepository geofenceRepository,
                           GeofenceService geofenceService,
                           AlertService alertService,
                           ApiLocationRequestAdapter apiLocationRequestAdapter,
                           DeviceLocationPayloadAdapter deviceLocationPayloadAdapter) {
        this.locationRepository = locationRepository;
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
        this.geofenceRepository = geofenceRepository;
        this.geofenceService = geofenceService;
        this.alertService = alertService;
        this.apiLocationRequestAdapter = apiLocationRequestAdapter;
        this.deviceLocationPayloadAdapter = deviceLocationPayloadAdapter;
    }

    @Transactional
    public LocationResponseDTO registerLocation(LocationRequestDTO requestDTO) {
        LocationCommand command = apiLocationRequestAdapter.adapt(requestDTO);
        return registerLocationInternal(command);
    }

    @Transactional
    public LocationResponseDTO registerLocationFromDevice(DeviceLocationPayloadDTO payloadDTO) {
        LocationCommand command = deviceLocationPayloadAdapter.adapt(payloadDTO);
        return registerLocationInternal(command);
    }

    private LocationResponseDTO registerLocationInternal(LocationCommand command) {
        validateCoordinates(command.getLatitude(), command.getLongitude());

        Collar collar = collarRepository.findByToken(command.getCollarToken())
                .orElseThrow(() -> new ResourceNotFoundException("Collar no registrado"));

        if (collar.getCow() == null) {
            throw new BadRequestException("El collar no está asociado a ninguna vaca");
        }

        Cow cow = collar.getCow();

        Location location = new Location();
        location.setLatitude(command.getLatitude());
        location.setLongitude(command.getLongitude());
        location.setTimestamp(command.getTimestamp());
        location.setCow(cow);
        location.setCollar(collar);

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

        return mapToResponseDTO(savedLocation);
    }

    public Page<LocationResponseDTO> getLocationHistoryByCow(Long cowId, int page, int size) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return locationRepository.findByCowOrderByTimestampDesc(cow, pageable)
                .map(this::mapToResponseDTO);
    }

    public Page<LocationResponseDTO> getLocationHistoryByCowAndDates(Long cowId,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end,
                                                                     int page,
                                                                     int size) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return locationRepository.findByCowAndTimestampBetweenOrderByTimestampDesc(cow, start, end, pageable)
                .map(this::mapToResponseDTO);
    }

    public LocationResponseDTO getLastLocationByCow(Long cowId) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaca no encontrada"));

        Location location = locationRepository.findTopByCowOrderByTimestampDesc(cow)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ubicación registrada para esa vaca"));

        return mapToResponseDTO(location);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("Latitud no válida");
        }

        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("Longitud no válida");
        }
    }

    private LocationResponseDTO mapToResponseDTO(Location location) {
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