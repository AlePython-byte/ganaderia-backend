package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CowIncidentReportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private final PaginationService paginationService = new PaginationService(new PaginationProperties());

    @Test
    void shouldReturnOperationalRecurrenceOrderedByPendingAndRecency() {
        CowIncidentReportService service = new CowIncidentReportService(alertRepository, paginationService);

        when(alertRepository.findAll(anyAlertSpecification(), any(Sort.class)))
                .thenReturn(List.of(
                        createAlert(1L, "VACA-001", "Luna", CowStatus.FUERA, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, LocalDateTime.of(2026, 4, 24, 10, 0)),
                        createAlert(1L, "VACA-001", "Luna", CowStatus.FUERA, AlertType.EXIT_GEOFENCE, AlertStatus.RESUELTA, LocalDateTime.of(2026, 4, 23, 10, 0)),
                        createAlert(2L, "VACA-002", "Canela", CowStatus.DENTRO, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, LocalDateTime.of(2026, 4, 25, 10, 0)),
                        createAlert(2L, "VACA-002", "Canela", CowStatus.DENTRO, AlertType.COLLAR_OFFLINE, AlertStatus.PENDIENTE, LocalDateTime.of(2026, 4, 24, 9, 0))
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

    @SuppressWarnings("unchecked")
    private Specification<Alert> anyAlertSpecification() {
        return any(Specification.class);
    }

    private Alert createAlert(Long cowId,
                              String cowToken,
                              String cowName,
                              CowStatus cowStatus,
                              AlertType type,
                              AlertStatus status,
                              LocalDateTime createdAt) {
        Cow cow = new Cow();
        cow.setId(cowId);
        cow.setToken(cowToken);
        cow.setInternalCode("INT-" + cowToken);
        cow.setName(cowName);
        cow.setStatus(cowStatus);

        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setType(type);
        alert.setStatus(status);
        alert.setMessage("incident");
        alert.setCreatedAt(createdAt);
        return alert;
    }
}
