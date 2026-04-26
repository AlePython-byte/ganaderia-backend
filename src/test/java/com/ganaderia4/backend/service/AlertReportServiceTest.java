package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.AlertTrendPointDTO;
import com.ganaderia4.backend.dto.AlertTypeRecurrenceDTO;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.AlertTrendAggregateProjection;
import com.ganaderia4.backend.repository.AlertTypeRecurrenceAggregateProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertReportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private final PaginationService paginationService = new PaginationService(new PaginationProperties());

    @Test
    void shouldReturnPagedAlertReportWithSafeSort() {
        AlertReportService service = new AlertReportService(alertRepository, paginationService);
        Alert alert = createAlert();

        when(alertRepository.findAll(anyAlertSpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alert)));

        Page<AlertResponseDTO> page = service.getAlertReportPage(
                new AlertReportFilterDTO(),
                0,
                20,
                "createdAt",
                "DESC"
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("COLLAR_OFFLINE", page.getContent().get(0).getType());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertRepository).findAll(anyAlertSpecification(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void shouldRejectPageSizeGreaterThanMaximum() {
        AlertReportService service = new AlertReportService(alertRepository, paginationService);

        assertThrows(BadRequestException.class, () -> service.getAlertReportPage(
                new AlertReportFilterDTO(),
                0,
                101,
                "createdAt",
                "DESC"
        ));
    }

    @Test
    void shouldRejectUnknownSortField() {
        AlertReportService service = new AlertReportService(alertRepository, paginationService);

        assertThrows(BadRequestException.class, () -> service.getAlertReportPage(
                new AlertReportFilterDTO(),
                0,
                20,
                "cow.password",
                "DESC"
        ));
    }

    @Test
    void shouldAggregateAlertTrendByDay() {
        AlertReportService service = new AlertReportService(alertRepository, paginationService);
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(LocalDateTime.of(2026, 4, 20, 0, 0));
        filter.setTo(LocalDateTime.of(2026, 4, 21, 23, 59));
        filter.setType(AlertType.COLLAR_OFFLINE);
        filter.setStatus(AlertStatus.PENDIENTE);

        when(alertRepository.findTrendAggregates(
                LocalDateTime.of(2026, 4, 20, 0, 0),
                LocalDateTime.of(2026, 4, 21, 23, 59),
                "COLLAR_OFFLINE",
                "PENDIENTE"
        ))
                .thenReturn(List.of(
                        alertTrendAggregate(java.time.LocalDate.of(2026, 4, 20), 2, 1, 1, 0),
                        alertTrendAggregate(java.time.LocalDate.of(2026, 4, 21), 1, 0, 0, 1)
                ));

        List<AlertTrendPointDTO> trend = service.getAlertTrendReport(filter);

        assertEquals(2, trend.size());
        assertEquals(java.time.LocalDate.of(2026, 4, 20), trend.get(0).getDate());
        assertEquals(2, trend.get(0).getTotalAlerts());
        assertEquals(1, trend.get(0).getPendingAlerts());
        assertEquals(1, trend.get(0).getResolvedAlerts());
        assertEquals(0, trend.get(0).getDiscardedAlerts());
        assertEquals(java.time.LocalDate.of(2026, 4, 21), trend.get(1).getDate());
        assertEquals(1, trend.get(1).getDiscardedAlerts());
    }

    @Test
    void shouldAggregateAlertRecurrenceByType() {
        AlertReportService service = new AlertReportService(alertRepository, paginationService);
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(LocalDateTime.of(2026, 4, 19, 0, 0));
        filter.setTo(LocalDateTime.of(2026, 4, 21, 23, 59));
        filter.setStatus(AlertStatus.RESUELTA);

        when(alertRepository.findTypeRecurrenceAggregates(
                LocalDateTime.of(2026, 4, 19, 0, 0),
                LocalDateTime.of(2026, 4, 21, 23, 59),
                null,
                "RESUELTA"
        ))
                .thenReturn(List.of(
                        alertTypeRecurrenceAggregate(
                                "COLLAR_OFFLINE",
                                2,
                                1,
                                1,
                                0,
                                LocalDateTime.of(2026, 4, 21, 10, 0)
                        ),
                        alertTypeRecurrenceAggregate(
                                "EXIT_GEOFENCE",
                                1,
                                0,
                                0,
                                1,
                                LocalDateTime.of(2026, 4, 19, 10, 0)
                        )
                ));

        List<AlertTypeRecurrenceDTO> recurrence = service.getAlertTypeRecurrenceReport(filter);

        assertEquals(2, recurrence.size());
        assertEquals("COLLAR_OFFLINE", recurrence.get(0).getType());
        assertEquals(2, recurrence.get(0).getTotalAlerts());
        assertEquals(1, recurrence.get(0).getPendingAlerts());
        assertEquals(1, recurrence.get(0).getResolvedAlerts());
        assertEquals(0, recurrence.get(0).getDiscardedAlerts());
        assertEquals(LocalDateTime.of(2026, 4, 21, 10, 0), recurrence.get(0).getLastAlertAt());
        assertEquals("EXIT_GEOFENCE", recurrence.get(1).getType());
        assertEquals(1, recurrence.get(1).getDiscardedAlerts());
    }

    private Alert createAlert() {
        return createAlert(AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, LocalDateTime.now());
    }

    private Alert createAlert(AlertType type, AlertStatus status, LocalDateTime createdAt) {
        Cow cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setInternalCode("INT-VACA-001");
        cow.setName("Luna");
        cow.setStatus(CowStatus.DENTRO);

        Alert alert = new Alert();
        alert.setId(10L);
        alert.setCow(cow);
        alert.setType(type);
        alert.setStatus(status);
        alert.setMessage("Collar sin senal");
        alert.setCreatedAt(createdAt);
        return alert;
    }

    private AlertTrendAggregateProjection alertTrendAggregate(java.time.LocalDate date,
                                                              long totalAlerts,
                                                              long pendingAlerts,
                                                              long resolvedAlerts,
                                                              long discardedAlerts) {
        return new AlertTrendAggregateProjection() {
            @Override
            public java.time.LocalDate getDate() {
                return date;
            }

            @Override
            public long getTotalAlerts() {
                return totalAlerts;
            }

            @Override
            public long getPendingAlerts() {
                return pendingAlerts;
            }

            @Override
            public long getResolvedAlerts() {
                return resolvedAlerts;
            }

            @Override
            public long getDiscardedAlerts() {
                return discardedAlerts;
            }
        };
    }

    private AlertTypeRecurrenceAggregateProjection alertTypeRecurrenceAggregate(String type,
                                                                                long totalAlerts,
                                                                                long pendingAlerts,
                                                                                long resolvedAlerts,
                                                                                long discardedAlerts,
                                                                                LocalDateTime lastAlertAt) {
        return new AlertTypeRecurrenceAggregateProjection() {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public long getTotalAlerts() {
                return totalAlerts;
            }

            @Override
            public long getPendingAlerts() {
                return pendingAlerts;
            }

            @Override
            public long getResolvedAlerts() {
                return resolvedAlerts;
            }

            @Override
            public long getDiscardedAlerts() {
                return discardedAlerts;
            }

            @Override
            public LocalDateTime getLastAlertAt() {
                return lastAlertAt;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Specification<Alert> anyAlertSpecification() {
        return any(Specification.class);
    }
}
