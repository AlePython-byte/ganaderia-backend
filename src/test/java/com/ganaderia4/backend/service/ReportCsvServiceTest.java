package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportCsvServiceTest {

    @Test
    void shouldRejectCsvExportWhenReportExceedsConfiguredMaximumRows() {
        AlertReportService alertReportService = mock(AlertReportService.class);
        AlertReportFilterDTO filter = new AlertReportFilterDTO();

        when(alertReportService.countAlertReport(filter)).thenReturn(2L);

        ReportCsvService service = new ReportCsvService(alertReportService, 1);

        assertThrows(BadRequestException.class, () -> service.exportAlertsReportCsv(filter));
        verify(alertReportService, never()).getAlertReport(filter);
    }

    @Test
    void shouldExportCsvWhenReportIsWithinConfiguredMaximumRows() {
        AlertReportService alertReportService = mock(AlertReportService.class);
        AlertReportFilterDTO filter = new AlertReportFilterDTO();

        when(alertReportService.countAlertReport(filter)).thenReturn(1L);
        when(alertReportService.getAlertReport(filter)).thenReturn(List.of(new AlertResponseDTO(
                10L,
                "COLLAR_OFFLINE",
                "Collar sin senal",
                LocalDateTime.of(2026, 4, 18, 10, 0),
                "PENDIENTE",
                null,
                20L,
                "VACA-001",
                "Luna",
                null
        )));

        ReportCsvService service = new ReportCsvService(alertReportService, 1);

        String csv = new String(service.exportAlertsReportCsv(filter), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("id,type,status,message,createdAt,observations,cowId,cowToken,cowName,locationId"));
        assertTrue(csv.contains("\"COLLAR_OFFLINE\""));
        assertTrue(csv.contains("\"VACA-001\""));
    }
}
