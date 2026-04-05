package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.pattern.adapter.location.ApiLocationRequestAdapter;
import com.ganaderia4.backend.pattern.adapter.location.DeviceLocationPayloadAdapter;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.facade.MonitoringFacade;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private ApiLocationRequestAdapter apiLocationRequestAdapter;

    @Mock
    private DeviceLocationPayloadAdapter deviceLocationPayloadAdapter;

    @Mock
    private MonitoringFacade monitoringFacade;

    @InjectMocks
    private LocationService locationService;

    @Test
    void shouldDelegateApiLocationProcessingToFacade() {
        LocationRequestDTO request = new LocationRequestDTO();
        request.setCollarToken("COL-001");
        request.setLatitude(2.0);
        request.setLongitude(2.0);
        request.setTimestamp(LocalDateTime.now());

        LocationCommand command = new LocationCommand("COL-001", 2.0, 2.0, request.getTimestamp());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        when(apiLocationRequestAdapter.adapt(request)).thenReturn(command);
        when(monitoringFacade.processLocation(command)).thenReturn(responseDTO);

        LocationResponseDTO response = locationService.registerLocation(request);

        assertNotNull(response);
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());

        verify(apiLocationRequestAdapter).adapt(request);
        verify(monitoringFacade).processLocation(command);
    }

    @Test
    void shouldDelegateApiLocationInsideFlowToFacade() {
        LocationRequestDTO request = new LocationRequestDTO();
        request.setCollarToken("COL-001");
        request.setLatitude(1.0);
        request.setLongitude(1.0);
        request.setTimestamp(LocalDateTime.now());

        LocationCommand command = new LocationCommand("COL-001", 1.0, 1.0, request.getTimestamp());

        LocationResponseDTO responseDTO = new LocationResponseDTO();
        responseDTO.setCowToken("VACA-001");
        responseDTO.setCollarToken("COL-001");

        when(apiLocationRequestAdapter.adapt(request)).thenReturn(command);
        when(monitoringFacade.processLocation(command)).thenReturn(responseDTO);

        LocationResponseDTO response = locationService.registerLocation(request);

        assertNotNull(response);
        assertEquals("VACA-001", response.getCowToken());
        assertEquals("COL-001", response.getCollarToken());

        verify(apiLocationRequestAdapter).adapt(request);
        verify(monitoringFacade).processLocation(command);
    }
}