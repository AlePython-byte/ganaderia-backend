package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.Geofence;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.pattern.adapter.location.ApiLocationRequestAdapter;
import com.ganaderia4.backend.pattern.adapter.location.DeviceLocationPayloadAdapter;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CollarRepository collarRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private GeofenceRepository geofenceRepository;

    @Mock
    private GeofenceService geofenceService;

    @Mock
    private AlertService alertService;

    @Mock
    private ApiLocationRequestAdapter apiLocationRequestAdapter;

    @Mock
    private DeviceLocationPayloadAdapter deviceLocationPayloadAdapter;

    @InjectMocks
    private LocationService locationService;

    private Cow cow;
    private Collar collar;
    private Geofence geofence;

    @BeforeEach
    void setUp() {
        cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setName("Luna");
        cow.setStatus(CowStatus.DENTRO);

        collar = new Collar();
        collar.setId(1L);
        collar.setToken("COL-001");
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setCow(cow);

        geofence = new Geofence();
        geofence.setId(20L);
        geofence.setName("Geocerca principal");
        geofence.setCenterLatitude(1.0);
        geofence.setCenterLongitude(1.0);
        geofence.setRadiusMeters(100.0);
        geofence.setActive(true);
        geofence.setCow(cow);
    }

    @Test
    void shouldCreateAlertAndSetCowOutsideWhenLocationIsOutsideGeofence() {
        LocationRequestDTO request = new LocationRequestDTO();
        request.setCollarToken("COL-001");
        request.setLatitude(2.0);
        request.setLongitude(2.0);
        request.setTimestamp(LocalDateTime.now());

        LocationCommand command = new LocationCommand("COL-001", 2.0, 2.0, request.getTimestamp());

        when(apiLocationRequestAdapter.adapt(request)).thenReturn(command);
        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setId(50L);
            return location;
        });

        when(geofenceRepository.findByCowAndActive(cow, true)).thenReturn(Optional.of(geofence));
        when(geofenceService.isInsideGeofence(command.getLatitude(), command.getLongitude(), geofence))
                .thenReturn(false);

        LocationResponseDTO response = locationService.registerLocation(request);

        assertNotNull(response);
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());
        assertEquals(CowStatus.FUERA, cow.getStatus());

        verify(apiLocationRequestAdapter).adapt(request);
        verify(alertService).createExitGeofenceAlert(eq(cow), any(Location.class));
        verify(cowRepository).save(cow);
    }

    @Test
    void shouldNotCreateAlertAndSetCowInsideWhenLocationIsInsideGeofence() {
        cow.setStatus(CowStatus.FUERA);

        LocationRequestDTO request = new LocationRequestDTO();
        request.setCollarToken("COL-001");
        request.setLatitude(1.0);
        request.setLongitude(1.0);
        request.setTimestamp(LocalDateTime.now());

        LocationCommand command = new LocationCommand("COL-001", 1.0, 1.0, request.getTimestamp());

        when(apiLocationRequestAdapter.adapt(request)).thenReturn(command);
        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setId(60L);
            return location;
        });

        when(geofenceRepository.findByCowAndActive(cow, true)).thenReturn(Optional.of(geofence));
        when(geofenceService.isInsideGeofence(command.getLatitude(), command.getLongitude(), geofence))
                .thenReturn(true);

        LocationResponseDTO response = locationService.registerLocation(request);

        assertNotNull(response);
        assertEquals("VACA-001", response.getCowToken());
        assertEquals(CowStatus.DENTRO, cow.getStatus());

        verify(apiLocationRequestAdapter).adapt(request);
        verify(alertService, never()).createExitGeofenceAlert(any(Cow.class), any(Location.class));
        verify(cowRepository).save(cow);
    }
}