package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CowIncidentAggregateView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CowIncidentReportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private final PaginationService paginationService = new PaginationService(new PaginationProperties());

    @Test
    void shouldReturnOperationalRecurrenceOrderedByPendingAndRecency() {
        CowIncidentReportService service = new CowIncidentReportService(alertRepository, paginationService);

        when(alertRepository.findCowIncidentAggregatesByOperationalRecurrence(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(
                        createAggregate(2L, "VACA-002", "Canela", CowStatus.DENTRO, 2, 2, 0, 0,
                                LocalDateTime.of(2026, 4, 24, 9, 0), LocalDateTime.of(2026, 4, 25, 10, 0)),
                        createAggregate(1L, "VACA-001", "Luna", CowStatus.FUERA, 2, 1, 1, 0,
                                LocalDateTime.of(2026, 4, 23, 10, 0), LocalDateTime.of(2026, 4, 24, 10, 0))
                ));
        when(alertRepository.findLatestIncidentTypesByCowIds(anyList(), any(), any(), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{2L, AlertType.COLLAR_OFFLINE},
                        new Object[]{1L, AlertType.COLLAR_OFFLINE}
                ));

        List<CowIncidentReportDTO> report = service.getCowIncidentRecurrenceReport(new AlertReportFilterDTO(), 10);

        assertEquals(2, report.size());
        assertEquals("VACA-002", report.get(0).getCowToken());
        assertEquals(2, report.get(0).getPendingIncidents());
        assertEquals("DENTRO", report.get(0).getCowStatus());
        assertEquals("COLLAR_OFFLINE", report.get(0).getLastIncidentType());
        assertEquals(LocalDateTime.of(2026, 4, 24, 9, 0), report.get(0).getFirstIncidentAt());
        assertEquals("VACA-001", report.get(1).getCowToken());
    }

    @Test
    void shouldReturnMostIncidentsReportUsingAggregateQuery() {
        CowIncidentReportService service = new CowIncidentReportService(alertRepository, paginationService);

        when(alertRepository.findCowIncidentAggregatesByTotalIncidents(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(
                        createAggregate(1L, "VACA-001", "Luna", CowStatus.FUERA, 3, 1, 1, 1,
                                LocalDateTime.of(2026, 4, 23, 10, 0), LocalDateTime.of(2026, 4, 25, 10, 0))
                ));
        when(alertRepository.findLatestIncidentTypesByCowIds(anyList(), any(), any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, AlertType.EXIT_GEOFENCE}));

        List<CowIncidentReportDTO> report = service.getCowsMostIncidentsReport(new AlertReportFilterDTO(), 10);

        assertEquals(1, report.size());
        assertEquals("VACA-001", report.get(0).getCowToken());
        assertEquals(3, report.get(0).getTotalIncidents());
        assertEquals(1, report.get(0).getPendingIncidents());
        assertEquals(1, report.get(0).getResolvedIncidents());
        assertEquals(1, report.get(0).getDiscardedIncidents());
        assertEquals("EXIT_GEOFENCE", report.get(0).getLastIncidentType());
    }

    private CowIncidentAggregateView createAggregate(Long cowId,
                                                     String cowToken,
                                                     String cowName,
                                                     CowStatus cowStatus,
                                                     long totalIncidents,
                                                     long pendingIncidents,
                                                     long resolvedIncidents,
                                                     long discardedIncidents,
                                                     LocalDateTime firstIncidentAt,
                                                     LocalDateTime lastIncidentAt) {
        return new CowIncidentAggregateView(
                cowId,
                cowToken,
                cowName,
                cowStatus,
                totalIncidents,
                pendingIncidents,
                resolvedIncidents,
                discardedIncidents,
                firstIncidentAt,
                lastIncidentAt
        );
    }
}
