package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
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

    @Test
    void shouldReturnPagedAlertReportWithSafeSort() {
        AlertReportService service = new AlertReportService(alertRepository);
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
        AlertReportService service = new AlertReportService(alertRepository);

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
        AlertReportService service = new AlertReportService(alertRepository);

        assertThrows(BadRequestException.class, () -> service.getAlertReportPage(
                new AlertReportFilterDTO(),
                0,
                20,
                "cow.password",
                "DESC"
        ));
    }

    private Alert createAlert() {
        Cow cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setInternalCode("INT-VACA-001");
        cow.setName("Luna");
        cow.setStatus(CowStatus.DENTRO);

        Alert alert = new Alert();
        alert.setId(10L);
        alert.setCow(cow);
        alert.setType(AlertType.COLLAR_OFFLINE);
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setMessage("Collar sin senal");
        alert.setCreatedAt(LocalDateTime.now());
        return alert;
    }

    @SuppressWarnings("unchecked")
    private Specification<Alert> anyAlertSpecification() {
        return any(Specification.class);
    }
}
