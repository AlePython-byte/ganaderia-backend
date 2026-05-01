package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.DeviceLocationPayloadDTO;
import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        verify(collarRepository, never()).findByToken(any());
        verify(alertService, never()).createLowBatteryAlert(any());
        verify(alertService, never()).resolvePendingLowBatteryAlert(any(), any());
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
}
