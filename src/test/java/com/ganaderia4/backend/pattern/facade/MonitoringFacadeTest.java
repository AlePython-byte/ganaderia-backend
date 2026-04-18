package com.ganaderia4.backend.pattern.facade;

import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import com.ganaderia4.backend.service.AlertService;
import com.ganaderia4.backend.service.GeofenceService;
import com.ganaderia4.backend.pattern.observer.geofence.GeofenceExitNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringFacadeTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private CollarRepository collarRepository;

    @Mock
    private GeofenceRepository geofenceRepository;

    @Mock
    private GeofenceService geofenceService;

    @Mock
    private GeofenceExitNotifier geofenceExitNotifier;

    @Mock
    private AlertService alertService;

    @Mock
    private LocationValidationChain validationChain;

    private MonitoringFacade monitoringFacade;

    @BeforeEach
    void setUp() {
        monitoringFacade = new MonitoringFacade(
                locationRepository,
                cowRepository,
                collarRepository,
                geofenceRepository,
                geofenceService,
                geofenceExitNotifier,
                alertService
        );
    }

    @Test
    void shouldReturnExistingLocationWhenDevicePayloadWasAlreadyRegistered() {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1).withNano(0);
        LocationCommand command = new LocationCommand("COL-001", 1.214, -77.281, timestamp);

        Cow cow = new Cow();
        cow.setId(20L);
        cow.setToken("VACA-001");
        cow.setName("Luna");
        cow.setStatus(CowStatus.DENTRO);

        Collar collar = new Collar();
        collar.setId(10L);
        collar.setToken("COL-001");
        collar.setCow(cow);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setSignalStatus(DeviceSignalStatus.MEDIA);

        Location existingLocation = new Location();
        existingLocation.setId(100L);
        existingLocation.setCow(cow);
        existingLocation.setCollar(collar);
        existingLocation.setLatitude(command.getLatitude());
        existingLocation.setLongitude(command.getLongitude());
        existingLocation.setTimestamp(command.getTimestamp());

        when(locationRepository.findFirstByCollarAndTimestampAndLatitudeAndLongitude(
                collar,
                command.getTimestamp(),
                command.getLatitude(),
                command.getLongitude()
        )).thenReturn(Optional.of(existingLocation));

        org.mockito.Mockito.doAnswer(invocation -> {
            com.ganaderia4.backend.pattern.chain.location.LocationValidationContext context = invocation.getArgument(0);
            context.setCollar(collar);
            context.setCow(cow);
            return null;
        }).when(validationChain).validate(any());

        LocationResponseDTO response = monitoringFacade.processLocation(command, validationChain);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());

        verify(locationRepository, never()).save(any(Location.class));
        verify(collarRepository, never()).save(any(Collar.class));
        verify(alertService, never()).resolvePendingCollarOfflineAlert(any(), any());
    }
}
