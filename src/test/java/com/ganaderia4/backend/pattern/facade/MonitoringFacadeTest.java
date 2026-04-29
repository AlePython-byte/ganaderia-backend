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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

        Cow cow = createCow();
        Collar collar = createCollar(cow, null);

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

        stubValidation(collar, cow);

        LocationResponseDTO response = monitoringFacade.processLocation(command, validationChain);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());

        verify(locationRepository, never()).save(any(Location.class));
        verify(collarRepository, never()).save(any(Collar.class));
        verify(alertService, never()).resolvePendingCollarOfflineAlert(any(), any());
    }

    @Test
    void shouldSetLastSeenAtWhenCurrentValueIsNull() {
        LocalDateTime incomingTimestamp = LocalDateTime.of(2026, 4, 28, 12, 0, 0);
        Cow cow = createCow();
        Collar collar = createCollar(cow, null);

        LocationResponseDTO response = processNewLocation(collar, cow, incomingTimestamp);

        assertEquals(incomingTimestamp, collar.getLastSeenAt());
        assertEquals(incomingTimestamp, response.getTimestamp());
        verify(alertService).resolvePendingCollarOfflineAlert(collar, incomingTimestamp);
    }

    @Test
    void shouldAdvanceLastSeenAtWhenIncomingTimestampIsNewer() {
        LocalDateTime currentLastSeenAt = LocalDateTime.of(2026, 4, 28, 12, 0, 0);
        LocalDateTime incomingTimestamp = currentLastSeenAt.plusMinutes(5);
        Cow cow = createCow();
        Collar collar = createCollar(cow, currentLastSeenAt);

        processNewLocation(collar, cow, incomingTimestamp);

        assertEquals(incomingTimestamp, collar.getLastSeenAt());
        verify(alertService).resolvePendingCollarOfflineAlert(collar, incomingTimestamp);
    }

    @Test
    void shouldKeepLastSeenAtWhenIncomingTimestampIsOlderButStillPersistLocationHistory() {
        LocalDateTime currentLastSeenAt = LocalDateTime.of(2026, 4, 28, 12, 0, 0);
        LocalDateTime incomingTimestamp = currentLastSeenAt.minusMinutes(10);
        Cow cow = createCow();
        Collar collar = createCollar(cow, currentLastSeenAt);

        LocationResponseDTO response = processNewLocation(collar, cow, incomingTimestamp);

        assertEquals(currentLastSeenAt, collar.getLastSeenAt());
        assertEquals(incomingTimestamp, response.getTimestamp());

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(locationCaptor.capture());
        assertEquals(incomingTimestamp, locationCaptor.getValue().getTimestamp());

        verify(alertService, never()).resolvePendingCollarOfflineAlert(collar, incomingTimestamp);
    }

    @Test
    void shouldKeepLastSeenAtWhenIncomingTimestampIsEqualAndNotResolveOfflineAlert() {
        LocalDateTime currentLastSeenAt = LocalDateTime.of(2026, 4, 28, 12, 0, 0);
        Cow cow = createCow();
        Collar collar = createCollar(cow, currentLastSeenAt);

        processNewLocation(collar, cow, currentLastSeenAt);

        assertEquals(currentLastSeenAt, collar.getLastSeenAt());
        verify(alertService, never()).resolvePendingCollarOfflineAlert(collar, currentLastSeenAt);
    }

    private LocationResponseDTO processNewLocation(Collar collar, Cow cow, LocalDateTime incomingTimestamp) {
        LocationCommand command = new LocationCommand("COL-001", 1.214, -77.281, incomingTimestamp);

        when(locationRepository.findFirstByCollarAndTimestampAndLatitudeAndLongitude(
                collar,
                command.getTimestamp(),
                command.getLatitude(),
                command.getLongitude()
        )).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setId(200L);
            return location;
        });
        when(geofenceRepository.findByCowAndActive(cow, true)).thenReturn(Optional.empty());

        stubValidation(collar, cow);

        LocationResponseDTO response = monitoringFacade.processLocation(command, validationChain);

        verify(collarRepository).save(collar);
        assertEquals(CollarStatus.ACTIVO, collar.getStatus());
        assertEquals(DeviceSignalStatus.MEDIA, collar.getSignalStatus());

        return response;
    }

    private void stubValidation(Collar collar, Cow cow) {
        doAnswer(invocation -> {
            com.ganaderia4.backend.pattern.chain.location.LocationValidationContext context = invocation.getArgument(0);
            context.setCollar(collar);
            context.setCow(cow);
            return null;
        }).when(validationChain).validate(any());
    }

    private Cow createCow() {
        Cow cow = new Cow();
        cow.setId(20L);
        cow.setToken("VACA-001");
        cow.setName("Luna");
        cow.setStatus(CowStatus.DENTRO);
        return cow;
    }

    private Collar createCollar(Cow cow, LocalDateTime lastSeenAt) {
        Collar collar = new Collar();
        collar.setId(10L);
        collar.setToken("COL-001");
        collar.setCow(cow);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setSignalStatus(DeviceSignalStatus.SIN_SENAL);
        collar.setLastSeenAt(lastSeenAt);
        return collar;
    }
}
