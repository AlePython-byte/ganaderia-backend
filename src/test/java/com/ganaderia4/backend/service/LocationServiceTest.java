package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.GpsAccuracyQuality;
import com.ganaderia4.backend.model.Location;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.pattern.abstractfactory.location.LocationProcessingFactory;
import com.ganaderia4.backend.pattern.abstractfactory.location.LocationProcessingFactoryProvider;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;
import com.ganaderia4.backend.pattern.facade.MonitoringFacade;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private CollarRepository collarRepository;

    @Mock
    private MonitoringFacade monitoringFacade;

    @Mock
    private LocationProcessingFactoryProvider locationProcessingFactoryProvider;

    @Mock
    private LocationProcessingFactory<LocationRequestDTO> apiLocationProcessingFactory;

    @Mock
    private LocationValidationChain locationValidationChain;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AlertService alertService;

    @Mock
    private DomainMetricsService domainMetricsService;

    @Spy
    private GpsAccuracyClassifier gpsAccuracyClassifier = new GpsAccuracyClassifier();

    @Spy
    private PaginationService paginationService = new PaginationService(new PaginationProperties());

    @InjectMocks
    private LocationService locationService;

    @Test
    void shouldDelegateApiLocationProcessingUsingAbstractFactory() {
        LocationRequestDTO request = new LocationRequestDTO();
        request.setCollarToken("COL-001");
        request.setLatitude(2.0);
        request.setLongitude(2.0);
        request.setTimestamp(LocalDateTime.now());

        LocationCommand command = new LocationCommand("COL-001", 2.0, 2.0, request.getTimestamp());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        when(locationProcessingFactoryProvider.<LocationRequestDTO>getFactory("API"))
                .thenReturn(apiLocationProcessingFactory);
        when(apiLocationProcessingFactory.createCommand(request)).thenReturn(command);
        when(apiLocationProcessingFactory.getValidationChain()).thenReturn(locationValidationChain);
        when(monitoringFacade.processLocation(command, locationValidationChain)).thenReturn(responseDTO);

        LocationResponseDTO response = locationService.registerLocation(request);

        assertNotNull(response);
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());

        verify(locationProcessingFactoryProvider).getFactory("API");
        verify(apiLocationProcessingFactory).createCommand(request);
        verify(apiLocationProcessingFactory).getValidationChain();
        verify(monitoringFacade).processLocation(command, locationValidationChain);
    }

    @Test
    void shouldDelegateAnotherApiLocationUsingAbstractFactory() {
        LocationRequestDTO request = new LocationRequestDTO();
        request.setCollarToken("COL-001");
        request.setLatitude(1.0);
        request.setLongitude(1.0);
        request.setTimestamp(LocalDateTime.now());

        LocationCommand command = new LocationCommand("COL-001", 1.0, 1.0, request.getTimestamp());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        when(locationProcessingFactoryProvider.<LocationRequestDTO>getFactory("API"))
                .thenReturn(apiLocationProcessingFactory);
        when(apiLocationProcessingFactory.createCommand(request)).thenReturn(command);
        when(apiLocationProcessingFactory.getValidationChain()).thenReturn(locationValidationChain);
        when(monitoringFacade.processLocation(command, locationValidationChain)).thenReturn(responseDTO);

        LocationResponseDTO response = locationService.registerLocation(request);

        assertNotNull(response);
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());

        verify(locationProcessingFactoryProvider).getFactory("API");
        verify(apiLocationProcessingFactory).createCommand(request);
        verify(apiLocationProcessingFactory).getValidationChain();
        verify(monitoringFacade).processLocation(command, locationValidationChain);
    }

    @Test
    void shouldRejectLocationHistoryPageSizeGreaterThanMaximum() {
        Cow cow = new Cow();
        cow.setId(1L);

        when(cowRepository.findById(1L)).thenReturn(Optional.of(cow));

        assertThrows(BadRequestException.class, () -> locationService.getLocationHistoryByCow(1L, 0, 101));

        verify(locationRepository, never()).findByCowOrderByTimestampDesc(any(), any());
    }

    @Test
    void shouldRegisterDeviceLocationWithoutBatteryLevelAndKeepPreviousBehavior() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.now());

        @SuppressWarnings("unchecked")
        LocationProcessingFactory<DeviceLocationPayloadDTO> deviceFactory = mock(LocationProcessingFactory.class);
        LocationValidationChain deviceValidationChain = mock(LocationValidationChain.class);
        LocationCommand command = new LocationCommand("COL-001", payload.getLat(), payload.getLon(), payload.getReportedAt());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        when(locationProcessingFactoryProvider.<DeviceLocationPayloadDTO>getFactory("DEVICE")).thenReturn(deviceFactory);
        when(deviceFactory.createCommand(payload)).thenReturn(command);
        when(deviceFactory.getValidationChain()).thenReturn(deviceValidationChain);
        when(monitoringFacade.processLocation(command, deviceValidationChain)).thenReturn(responseDTO);

        LocationResponseDTO response = locationService.registerLocationFromDevice(payload);

        assertNotNull(response);
        assertNull(response.getGpsAccuracy());
        verify(domainMetricsService).incrementGpsAccuracyQuality(GpsAccuracyQuality.UNKNOWN);
        verify(locationRepository, never()).findById(any());
        verify(collarRepository, never()).findByToken(any());
        verify(alertService, never()).createLowBatteryAlert(any());
        verify(alertService, never()).resolvePendingLowBatteryAlert(any(), any());
    }

    @Test
    void shouldPersistGpsAccuracyWhenProvidedInDeviceTelemetry() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        payload.setGpsAccuracy(4.5);

        @SuppressWarnings("unchecked")
        LocationProcessingFactory<DeviceLocationPayloadDTO> deviceFactory = mock(LocationProcessingFactory.class);
        LocationValidationChain deviceValidationChain = mock(LocationValidationChain.class);
        LocationCommand command = new LocationCommand("COL-001", payload.getLat(), payload.getLon(), payload.getReportedAt());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setId(55L);
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        Location persistedLocation = new Location();
        persistedLocation.setId(55L);

        when(locationProcessingFactoryProvider.<DeviceLocationPayloadDTO>getFactory("DEVICE")).thenReturn(deviceFactory);
        when(deviceFactory.createCommand(payload)).thenReturn(command);
        when(deviceFactory.getValidationChain()).thenReturn(deviceValidationChain);
        when(monitoringFacade.processLocation(command, deviceValidationChain)).thenReturn(responseDTO);
        when(locationRepository.findById(55L)).thenReturn(Optional.of(persistedLocation));

        LocationResponseDTO response = locationService.registerLocationFromDevice(payload);

        assertEquals(4.5, persistedLocation.getGpsAccuracy());
        assertEquals(4.5, response.getGpsAccuracy());
        verify(domainMetricsService).incrementGpsAccuracyQuality(GpsAccuracyQuality.GOOD);
        verify(locationRepository).save(persistedLocation);
        verify(collarRepository, never()).findByToken(any());
    }

    @Test
    void shouldExposeGpsAccuracyInLastLocationResponse() {
        Cow cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setName("Luna");

        Collar collar = new Collar();
        collar.setToken("COL-001");

        Location location = new Location();
        location.setId(55L);
        location.setLatitude(1.214);
        location.setLongitude(-77.281);
        location.setGpsAccuracy(4.5);
        location.setTimestamp(LocalDateTime.of(2026, 5, 1, 10, 0));
        location.setCow(cow);
        location.setCollar(collar);

        when(cowRepository.findById(1L)).thenReturn(Optional.of(cow));
        when(locationRepository.findTopByCowOrderByTimestampDesc(cow)).thenReturn(Optional.of(location));

        LocationResponseDTO response = locationService.getLastLocationByCow(1L);

        assertEquals(4.5, response.getGpsAccuracy());
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());
    }

    @Test
    void shouldKeepNullGpsAccuracyInLocationHistoryResponse() {
        Cow cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setName("Luna");

        Collar collar = new Collar();
        collar.setToken("COL-001");

        Location location = new Location();
        location.setId(56L);
        location.setLatitude(1.215);
        location.setLongitude(-77.282);
        location.setGpsAccuracy(null);
        location.setTimestamp(LocalDateTime.of(2026, 5, 1, 11, 0));
        location.setCow(cow);
        location.setCollar(collar);

        when(cowRepository.findById(1L)).thenReturn(Optional.of(cow));
        when(locationRepository.findByCowOrderByTimestampDesc(eq(cow), any()))
                .thenReturn(new PageImpl<>(List.of(location)));

        Page<LocationResponseDTO> response = locationService.getLocationHistoryByCow(1L, 0, 20);

        assertEquals(1, response.getTotalElements());
        assertNull(response.getContent().get(0).getGpsAccuracy());
        assertEquals("VACA-001", response.getContent().get(0).getCowToken());
    }

    @Test
    void shouldIncrementModerateGpsAccuracyMetricWhenTelemetryQualityIsModerate() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        payload.setGpsAccuracy(20.0);

        @SuppressWarnings("unchecked")
        LocationProcessingFactory<DeviceLocationPayloadDTO> deviceFactory = mock(LocationProcessingFactory.class);
        LocationValidationChain deviceValidationChain = mock(LocationValidationChain.class);
        LocationCommand command = new LocationCommand("COL-001", payload.getLat(), payload.getLon(), payload.getReportedAt());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setId(56L);
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        Location persistedLocation = new Location();
        persistedLocation.setId(56L);

        when(locationProcessingFactoryProvider.<DeviceLocationPayloadDTO>getFactory("DEVICE")).thenReturn(deviceFactory);
        when(deviceFactory.createCommand(payload)).thenReturn(command);
        when(deviceFactory.getValidationChain()).thenReturn(deviceValidationChain);
        when(monitoringFacade.processLocation(command, deviceValidationChain)).thenReturn(responseDTO);
        when(locationRepository.findById(56L)).thenReturn(Optional.of(persistedLocation));

        locationService.registerLocationFromDevice(payload);

        verify(domainMetricsService).incrementGpsAccuracyQuality(GpsAccuracyQuality.MODERATE);
        verify(locationRepository).save(persistedLocation);
    }

    @Test
    void shouldLogLowGpsAccuracyWhenTelemetryQualityIsLow(CapturedOutput output) {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        payload.setGpsAccuracy(30.1);

        @SuppressWarnings("unchecked")
        LocationProcessingFactory<DeviceLocationPayloadDTO> deviceFactory = mock(LocationProcessingFactory.class);
        LocationValidationChain deviceValidationChain = mock(LocationValidationChain.class);
        LocationCommand command = new LocationCommand("COL-001", payload.getLat(), payload.getLon(), payload.getReportedAt());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setId(55L);
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        Location persistedLocation = new Location();
        persistedLocation.setId(55L);

        when(locationProcessingFactoryProvider.<DeviceLocationPayloadDTO>getFactory("DEVICE")).thenReturn(deviceFactory);
        when(deviceFactory.createCommand(payload)).thenReturn(command);
        when(deviceFactory.getValidationChain()).thenReturn(deviceValidationChain);
        when(monitoringFacade.processLocation(command, deviceValidationChain)).thenReturn(responseDTO);
        when(locationRepository.findById(55L)).thenReturn(Optional.of(persistedLocation));

        locationService.registerLocationFromDevice(payload);

        String logs = output.getOut() + output.getErr();
        verify(domainMetricsService).incrementGpsAccuracyQuality(GpsAccuracyQuality.LOW);
        verify(locationRepository).save(persistedLocation);
        assertEquals(30.1, persistedLocation.getGpsAccuracy());
        org.junit.jupiter.api.Assertions.assertTrue(logs.contains("event=gps_accuracy_low"));
        org.junit.jupiter.api.Assertions.assertTrue(logs.contains("requestId=-"));
        org.junit.jupiter.api.Assertions.assertTrue(logs.contains("locationId=55"));
        org.junit.jupiter.api.Assertions.assertTrue(logs.contains("gpsAccuracy=30.1"));
        org.junit.jupiter.api.Assertions.assertTrue(logs.contains("quality=LOW"));
        org.junit.jupiter.api.Assertions.assertFalse(logs.contains("COL-001"));
    }

    @Test
    void shouldUpdateCollarBatteryAndCreateLowBatteryAlertFromDeviceTelemetry() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.of(2026, 4, 29, 10, 0));
        payload.setBatteryLevel(18);

        @SuppressWarnings("unchecked")
        LocationProcessingFactory<DeviceLocationPayloadDTO> deviceFactory = mock(LocationProcessingFactory.class);
        LocationValidationChain deviceValidationChain = mock(LocationValidationChain.class);
        LocationCommand command = new LocationCommand("COL-001", payload.getLat(), payload.getLon(), payload.getReportedAt());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        Collar collar = new Collar();
        collar.setToken("COL-001");

        when(locationProcessingFactoryProvider.<DeviceLocationPayloadDTO>getFactory("DEVICE")).thenReturn(deviceFactory);
        when(deviceFactory.createCommand(payload)).thenReturn(command);
        when(deviceFactory.getValidationChain()).thenReturn(deviceValidationChain);
        when(monitoringFacade.processLocation(command, deviceValidationChain)).thenReturn(responseDTO);
        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        locationService.registerLocationFromDevice(payload);

        assertEquals(18, collar.getBatteryLevel());
        verify(collarRepository).save(collar);
        verify(alertService).createLowBatteryAlert(collar);
        verify(alertService, never()).resolvePendingLowBatteryAlert(any(), any());
    }

    @Test
    void shouldResolveLowBatteryAlertWhenBatteryRecoversFromDeviceTelemetry() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.of(2026, 4, 29, 10, 0));
        payload.setBatteryLevel(40);

        @SuppressWarnings("unchecked")
        LocationProcessingFactory<DeviceLocationPayloadDTO> deviceFactory = mock(LocationProcessingFactory.class);
        LocationValidationChain deviceValidationChain = mock(LocationValidationChain.class);
        LocationCommand command = new LocationCommand("COL-001", payload.getLat(), payload.getLon(), payload.getReportedAt());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        Collar collar = new Collar();
        collar.setToken("COL-001");

        when(locationProcessingFactoryProvider.<DeviceLocationPayloadDTO>getFactory("DEVICE")).thenReturn(deviceFactory);
        when(deviceFactory.createCommand(payload)).thenReturn(command);
        when(deviceFactory.getValidationChain()).thenReturn(deviceValidationChain);
        when(monitoringFacade.processLocation(command, deviceValidationChain)).thenReturn(responseDTO);
        when(collarRepository.findByToken("COL-001")).thenReturn(Optional.of(collar));

        locationService.registerLocationFromDevice(payload);

        assertEquals(40, collar.getBatteryLevel());
        verify(collarRepository).save(collar);
        verify(alertService, never()).createLowBatteryAlert(any());
        verify(alertService).resolvePendingLowBatteryAlert(collar, payload.getReportedAt());
    }

    @Test
    void shouldRejectDevicePayloadWhenBatteryLevelIsOutOfRange() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.now());
        payload.setBatteryLevel(101);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> locationService.registerLocationFromDevice(payload)
        );

        assertEquals("El batteryLevel debe estar entre 0 y 100", exception.getMessage());
        verify(monitoringFacade, never()).processLocation(any(), any());
        verify(collarRepository, never()).findByToken(any());
    }

    @Test
    void shouldRejectDevicePayloadWhenGpsAccuracyIsNegative() {
        DeviceLocationPayloadDTO payload = new DeviceLocationPayloadDTO();
        payload.setDeviceToken("COL-001");
        payload.setLat(1.214);
        payload.setLon(-77.281);
        payload.setReportedAt(LocalDateTime.now());
        payload.setGpsAccuracy(-0.1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> locationService.registerLocationFromDevice(payload)
        );

        assertEquals("El gpsAccuracy no puede ser menor a 0", exception.getMessage());
        verify(monitoringFacade, never()).processLocation(any(), any());
        verify(locationRepository, never()).findById(any());
        verify(collarRepository, never()).findByToken(any());
    }
}
