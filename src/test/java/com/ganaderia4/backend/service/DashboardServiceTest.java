package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CowRepository cowRepository;

    @Mock
    private CollarRepository collarRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnCriticalAlertsFromPrioritizedQueueWithCompatibilityLimit() {
        List<AlertResponseDTO> prioritizedAlerts = List.of(
                createAlertResponse(2L, 82, "HIGH"),
                createAlertResponse(1L, 55, "MEDIUM")
        );

        when(alertService.getPendingAlertPriorityQueue(10)).thenReturn(prioritizedAlerts);

        List<AlertResponseDTO> response = dashboardService.getCriticalAlerts();

        assertEquals(2, response.size());
        assertEquals(2L, response.get(0).getId());
        assertEquals("HIGH", response.get(0).getPriority());
        verify(alertService).getPendingAlertPriorityQueue(10);
    }

    @Test
    void shouldExposeDashboardPrioritizedAlertQueueWithRequestedLimit() {
        List<AlertResponseDTO> prioritizedAlerts = List.of(createAlertResponse(3L, 90, "HIGH"));

        when(alertService.getPendingAlertPriorityQueue(1)).thenReturn(prioritizedAlerts);

        List<AlertResponseDTO> response = dashboardService.getPrioritizedAlertQueue(1);

        assertEquals(1, response.size());
        assertEquals(3L, response.get(0).getId());
        assertEquals(90, response.get(0).getPriorityScore());
        verify(alertService).getPendingAlertPriorityQueue(1);
    }

    private AlertResponseDTO createAlertResponse(Long id, Integer priorityScore, String priority) {
        return new AlertResponseDTO(
                id,
                "EXIT_GEOFENCE",
                "Alerta " + id,
                LocalDateTime.now(),
                "PENDIENTE",
                null,
                1L,
                "VACA-001",
                "Luna",
                null,
                priorityScore,
                priority
        );
    }
}
