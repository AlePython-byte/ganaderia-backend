package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.OfflineCollarReportDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.CollarRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollarReportServiceTest {

    @Test
    void shouldReturnOfflineCollarsOrderedByStaleness() {
        CollarRepository collarRepository = mock(CollarRepository.class);
        CollarReportService service = new CollarReportService(collarRepository, 15);

        Collar neverReported = createCollar("COLLAR-001", null);
        Collar staleForHours = createCollar("COLLAR-002", LocalDateTime.now().minusHours(7));
        Collar staleForMinutes = createCollar("COLLAR-003", LocalDateTime.now().minusMinutes(40));

        when(collarRepository.findByEnabledTrueAndSignalStatus(DeviceSignalStatus.SIN_SENAL))
                .thenReturn(List.of(staleForMinutes, staleForHours, neverReported));

        List<OfflineCollarReportDTO> report = service.getOfflineCollarsStalenessReport();

        assertEquals(3, report.size());
        assertEquals("COLLAR-001", report.get(0).getCollarToken());
        assertEquals("NEVER_REPORTED", report.get(0).getStalenessBucket());
        assertEquals("COLLAR-002", report.get(1).getCollarToken());
        assertEquals("OFFLINE_GT_6H", report.get(1).getStalenessBucket());
        assertEquals("COLLAR-003", report.get(2).getCollarToken());
        assertEquals("OFFLINE_GT_THRESHOLD", report.get(2).getStalenessBucket());
    }

    private Collar createCollar(String token, LocalDateTime lastSeenAt) {
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setEnabled(true);
        collar.setSignalStatus(DeviceSignalStatus.SIN_SENAL);
        collar.setLastSeenAt(lastSeenAt);
        return collar;
    }
}
