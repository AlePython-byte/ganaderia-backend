package com.ganaderia4.backend.pattern.facade;

import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.builder.LocationResponseDTOBuilder;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationContext;
import com.ganaderia4.backend.pattern.observer.geofence.GeofenceExitEvent;
import com.ganaderia4.backend.pattern.observer.geofence.GeofenceExitNotifier;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import com.ganaderia4.backend.service.GeofenceService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MonitoringFacade {

    private final LocationRepository locationRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeofenceService geofenceService;
    private final GeofenceExitNotifier geofenceExitNotifier;

    public MonitoringFacade(LocationRepository locationRepository,
                            CowRepository cowRepository,
                            GeofenceRepository geofenceRepository,
                            GeofenceService geofenceService,
                            GeofenceExitNotifier geofenceExitNotifier) {
        this.locationRepository = locationRepository;
        this.cowRepository = cowRepository;
        this.geofenceRepository = geofenceRepository;
        this.geofenceService = geofenceService;
        this.geofenceExitNotifier = geofenceExitNotifier;
    }

    @Transactional
    public LocationResponseDTO processLocation(LocationCommand command, LocationValidationChain validationChain) {
        LocationValidationContext validationContext = new LocationValidationContext(command);
        validationChain.validate(validationContext);

        Collar collar = validationContext.getCollar();
        Cow cow = validationContext.getCow();

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
}