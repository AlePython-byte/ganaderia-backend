package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-04-28T18:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

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

    @Mock
    private CowIncidentReportService cowIncidentReportService;

    private DashboardService dashboardService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                alertRepository,
                cowRepository,
                collarRepository,
                locationRepository,
                alertService,
                cowIncidentReportService,
                FIXED_CLOCK
        );
    }

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

    @Test
    void shouldReturnPendingAlertAgingBuckets() {
        when(alertRepository.countByStatus(com.ganaderia4.backend.model.AlertStatus.PENDIENTE)).thenReturn(12L);
        when(alertRepository.countByStatusAndCreatedAtBefore(eq(com.ganaderia4.backend.model.AlertStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(9L, 4L, 1L);

        var response = dashboardService.getPendingAlertAging();

        assertEquals(12L, response.getPendingAlerts());
        assertEquals(9L, response.getOlderThan15Minutes());
        assertEquals(4L, response.getOlderThan1Hour());
        assertEquals(1L, response.getOlderThan6Hours());
        verify(alertRepository).countByStatusAndCreatedAtBefore(
                com.ganaderia4.backend.model.AlertStatus.PENDIENTE,
                FIXED_NOW.minusMinutes(15)
        );
        verify(alertRepository).countByStatusAndCreatedAtBefore(
                com.ganaderia4.backend.model.AlertStatus.PENDIENTE,
                FIXED_NOW.minusHours(1)
        );
        verify(alertRepository).countByStatusAndCreatedAtBefore(
                com.ganaderia4.backend.model.AlertStatus.PENDIENTE,
                FIXED_NOW.minusHours(6)
        );
    }

    @Test
    void shouldReturnTelemetryFreshnessBucketsUsingOperationalThreshold() {
        ReflectionTestUtils.setField(dashboardService, "offlineThresholdMinutes", 15L);

        when(collarRepository.countByEnabledTrue()).thenReturn(8L);
        when(collarRepository.countByEnabledTrueAndLastSeenAtIsNull()).thenReturn(2L);
        when(collarRepository.countByEnabledTrueAndLastSeenAtGreaterThanEqual(any(LocalDateTime.class))).thenReturn(3L);
        when(collarRepository.countByEnabledTrueAndLastSeenAtBefore(any(LocalDateTime.class))).thenReturn(5L, 4L, 1L);

        var response = dashboardService.getTelemetryFreshness();

        assertEquals(8L, response.getEnabledCollars());
        assertEquals(2L, response.getNeverReported());
        assertEquals(3L, response.getReportingWithinThreshold());
        assertEquals(5L, response.getLastSeenOlderThanThreshold());
        assertEquals(4L, response.getLastSeenOlderThan1Hour());
        assertEquals(1L, response.getLastSeenOlderThan6Hours());
        assertEquals(15L, response.getOperationalThresholdMinutes());
        verify(collarRepository).countByEnabledTrueAndLastSeenAtGreaterThanEqual(FIXED_NOW.minusMinutes(15));
        verify(collarRepository).countByEnabledTrueAndLastSeenAtBefore(FIXED_NOW.minusMinutes(15));
        verify(collarRepository).countByEnabledTrueAndLastSeenAtBefore(FIXED_NOW.minusHours(1));
        verify(collarRepository).countByEnabledTrueAndLastSeenAtBefore(FIXED_NOW.minusHours(6));
    }

    @Test
    void shouldExposeTopProblematicCowsFromIncidentReportService() {
        List<CowIncidentReportDTO> topProblematicCows = List.of(
                new CowIncidentReportDTO(2L, "VACA-002", "Estrella", 7, 3, 3, 1, FIXED_NOW),
                new CowIncidentReportDTO(1L, "VACA-001", "Luna", 5, 2, 2, 1, FIXED_NOW.minusHours(1))
        );

        when(cowIncidentReportService.getCowsMostIncidentsReport(null, 2)).thenReturn(topProblematicCows);

        List<CowIncidentReportDTO> response = dashboardService.getTopProblematicCows(2);

        assertEquals(2, response.size());
        assertEquals("VACA-002", response.get(0).getCowToken());
        assertEquals(7, response.get(0).getTotalIncidents());
        verify(cowIncidentReportService).getCowsMostIncidentsReport(null, 2);
    }

    private AlertResponseDTO createAlertResponse(Long id, Integer priorityScore, String priority) {
        return new AlertResponseDTO(
                id,
                "EXIT_GEOFENCE",
                "Alerta " + id,
                FIXED_NOW,
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
