package com.ganaderia4.backend.pattern.facade;

import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.builder.LocationResponseDTOBuilder;
import com.ganaderia4.backend.pattern.observer.geofence.GeofenceExitEvent;
import com.ganaderia4.backend.pattern.observer.geofence.GeofenceExitNotifier;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import com.ganaderia4.backend.service.GeofenceService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MonitoringFacade {

    private final LocationRepository locationRepository;
    private final CollarRepository collarRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeofenceService geofenceService;
    private final GeofenceExitNotifier geofenceExitNotifier;

    public MonitoringFacade(LocationRepository locationRepository,
                            CollarRepository collarRepository,
                            CowRepository cowRepository,
                            GeofenceRepository geofenceRepository,
                            GeofenceService geofenceService,
                            GeofenceExitNotifier geofenceExitNotifier) {
        this.locationRepository = locationRepository;
        this.collarRepository = collarRepository;
        this.cowRepository = cowRepository;
        this.geofenceRepository = geofenceRepository;
        this.geofenceService = geofenceService;
        this.geofenceExitNotifier = geofenceExitNotifier;
    }

    @Transactional
    public LocationResponseDTO processLocation(LocationCommand command) {
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
                geofenceExitNotifier.notifyExit(new GeofenceExitEvent(cow, savedLocation));
            } else {
                cow.setStatus(CowStatus.DENTRO);
                cowRepository.save(cow);
            }
        });

        return new LocationResponseDTOBuilder()
                .id(savedLocation.getId())
                .latitude(savedLocation.getLatitude())
                .longitude(savedLocation.getLongitude())
                .timestamp(savedLocation.getTimestamp())
                .cowId(savedLocation.getCow().getId())
                .cowToken(savedLocation.getCow().getToken())
                .cowName(savedLocation.getCow().getName())
                .collarToken(savedLocation.getCollar() != null ? savedLocation.getCollar().getToken() : null)
                .build();
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("Latitud no válida");
        }

        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("Longitud no válida");
        }
    }
}