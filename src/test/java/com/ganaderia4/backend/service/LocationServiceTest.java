package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LocationRequestDTO;
import com.ganaderia4.backend.dto.LocationResponseDTO;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.pattern.abstractfactory.location.LocationProcessingFactory;
import com.ganaderia4.backend.pattern.abstractfactory.location.LocationProcessingFactoryProvider;
import com.ganaderia4.backend.pattern.adapter.location.LocationCommand;
import com.ganaderia4.backend.pattern.chain.location.LocationValidationChain;
import com.ganaderia4.backend.pattern.facade.MonitoringFacade;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CowRepository cowRepository;

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
}
