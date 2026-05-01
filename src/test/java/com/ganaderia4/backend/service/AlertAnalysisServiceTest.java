package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertAnalysisSummaryDTO;
import com.ganaderia4.backend.dto.AlertPriorityRecommendationDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertAnalysisServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertPriorityScorer alertPriorityScorer;

    private AlertAnalysisService alertAnalysisService;

    @BeforeEach
    void setUp() {
        alertAnalysisService = new AlertAnalysisService(alertRepository, alertPriorityScorer);
        lenient().when(alertPriorityScorer.buildContext(anyList()))
                .thenReturn(AlertPriorityScorer.AlertPriorityScoringContext.empty(LocalDateTime.now()));
    }

    @Test
    void shouldReturnLowRiskWhenThereAreNoPendingAlerts() {
        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of());

        AlertAnalysisSummaryDTO summary = alertAnalysisService.getSummary();

        assertEquals(AlertAnalysisRiskLevel.LOW, summary.getRiskLevel());
        assertEquals(0, summary.getTotalPendingAlerts());
        assertTrue(summary.getCriticalSignals().isEmpty());
        assertEquals(List.of("No hay acciones críticas pendientes en este momento."), summary.getRecommendedActions());
        assertEquals(AlertAnalysisService.CONFIDENCE_RULE_BASED, summary.getConfidence());
    }

    @Test
    void shouldReturnMediumRiskWhenThereIsOnlyOneLowBatteryAlert() {
        Alert lowBattery = pendingAlert(1L, AlertType.LOW_BATTERY);
        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(lowBattery));
        when(alertRepository.findTop10ByStatusOrderByCreatedAtDesc(AlertStatus.PENDIENTE)).thenReturn(List.of(lowBattery));
        lenient().when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.eq(lowBattery), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(35, "LOW"));

        AlertAnalysisSummaryDTO summary = alertAnalysisService.getSummary();

        assertEquals(AlertAnalysisRiskLevel.MEDIUM, summary.getRiskLevel());
        assertTrue(summary.getCriticalSignals().contains("Hay alertas de batería baja pendientes."));
        assertTrue(summary.getRecommendedActions().contains(
                "Programar recarga o reemplazo de batería en collares con batería baja."
        ));
    }

    @Test
    void shouldReturnHighRiskWhenThereIsPendingOfflineAlert() {
        Alert offline = pendingAlert(1L, AlertType.COLLAR_OFFLINE);
        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(offline));
        when(alertRepository.findTop10ByStatusOrderByCreatedAtDesc(AlertStatus.PENDIENTE)).thenReturn(List.of(offline));
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.eq(offline), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(45, "MEDIUM"));

        AlertAnalysisSummaryDTO summary = alertAnalysisService.getSummary();

        assertEquals(AlertAnalysisRiskLevel.HIGH, summary.getRiskLevel());
        assertTrue(summary.getCriticalSignals().contains("Hay collares offline pendientes."));
    }

    @Test
    void shouldReturnCriticalRiskWhenOfflineAndLowBatteryAreCombined() {
        Alert offline = pendingAlert(1L, AlertType.COLLAR_OFFLINE);
        Alert lowBattery = pendingAlert(2L, AlertType.LOW_BATTERY);

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(offline, lowBattery));
        when(alertRepository.findTop10ByStatusOrderByCreatedAtDesc(AlertStatus.PENDIENTE))
                .thenReturn(List.of(lowBattery, offline));
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.any(Alert.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(45, "MEDIUM"));

        AlertAnalysisSummaryDTO summary = alertAnalysisService.getSummary();

        assertEquals(AlertAnalysisRiskLevel.CRITICAL, summary.getRiskLevel());
        assertTrue(summary.getCriticalSignals().contains("Hay collares offline pendientes."));
        assertTrue(summary.getCriticalSignals().contains("Hay alertas de batería baja pendientes."));
    }

    @Test
    void shouldReturnCriticalRiskWhenPendingVolumeIsHigh() {
        List<Alert> alerts = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> pendingAlert((long) i + 1, AlertType.LOW_BATTERY))
                .toList();

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(alerts);
        when(alertRepository.findTop10ByStatusOrderByCreatedAtDesc(AlertStatus.PENDIENTE)).thenReturn(alerts);
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.any(Alert.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(35, "LOW"));

        AlertAnalysisSummaryDTO summary = alertAnalysisService.getSummary();

        assertEquals(AlertAnalysisRiskLevel.CRITICAL, summary.getRiskLevel());
        assertTrue(summary.getCriticalSignals().contains("El volumen de alertas pendientes es alto."));
        assertTrue(summary.getRecommendedActions().contains(
                "Priorizar la atención usando la cola de alertas por prioridad."
        ));
    }

    @Test
    void shouldReturnEmptyTopPrioritiesWhenThereAreNoPendingAlerts() {
        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of());

        List<AlertPriorityRecommendationDTO> response = alertAnalysisService.getTopPriorities(null);

        assertTrue(response.isEmpty());
    }

    @Test
    void shouldOrderTopPrioritiesByDescendingPriorityScore() {
        Alert lowBattery = pendingAlert(1L, AlertType.LOW_BATTERY);
        lowBattery.setId(10L);
        Alert offline = pendingAlert(2L, AlertType.COLLAR_OFFLINE);
        offline.setId(11L);
        Alert exit = pendingAlert(3L, AlertType.EXIT_GEOFENCE);
        exit.setId(12L);

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(List.of(lowBattery, offline, exit));
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.eq(lowBattery), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(35, "LOW"));
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.eq(offline), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(70, "HIGH"));
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.eq(exit), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(55, "MEDIUM"));

        List<AlertPriorityRecommendationDTO> response = alertAnalysisService.getTopPriorities(5);

        assertEquals(3, response.size());
        assertEquals(11L, response.get(0).getAlertId());
        assertEquals("HIGH", response.get(0).getPriorityLabel());
        assertEquals(12L, response.get(1).getAlertId());
        assertEquals(10L, response.get(2).getAlertId());
    }

    @Test
    void shouldUseDefaultLimitForTopPriorities() {
        List<Alert> alerts = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> {
                    Alert alert = pendingAlert((long) i + 1, AlertType.COLLAR_OFFLINE);
                    alert.setId((long) i + 1);
                    return alert;
                })
                .toList();

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(alerts);
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.any(Alert.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(50, "MEDIUM"));

        List<AlertPriorityRecommendationDTO> response = alertAnalysisService.getTopPriorities(null);

        assertEquals(5, response.size());
    }

    @Test
    void shouldRespectExplicitLimitForTopPriorities() {
        List<Alert> alerts = java.util.stream.IntStream.range(0, 3)
                .mapToObj(i -> {
                    Alert alert = pendingAlert((long) i + 1, AlertType.COLLAR_OFFLINE);
                    alert.setId((long) i + 1);
                    return alert;
                })
                .toList();

        when(alertRepository.findByStatus(AlertStatus.PENDIENTE)).thenReturn(alerts);
        when(alertPriorityScorer.score(org.mockito.ArgumentMatchers.any(Alert.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AlertPriorityAssessment(50, "MEDIUM"));

        List<AlertPriorityRecommendationDTO> response = alertAnalysisService.getTopPriorities(1);

        assertEquals(1, response.size());
    }

    @Test
    void shouldRejectTopPrioritiesWhenLimitIsLessThanOne() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertAnalysisService.getTopPriorities(0)
        );

        assertEquals("El limit debe estar entre 1 y 20", exception.getMessage());
    }

    @Test
    void shouldRejectTopPrioritiesWhenLimitExceedsMaximum() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> alertAnalysisService.getTopPriorities(21)
        );

        assertEquals("El limit debe estar entre 1 y 20", exception.getMessage());
    }

    private Alert pendingAlert(Long cowId, AlertType type) {
        Cow cow = new Cow();
        cow.setId(cowId);
        cow.setToken("VACA-" + cowId);

        Alert alert = new Alert();
        alert.setCow(cow);
        alert.setType(type);
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        alert.setMessage("Alerta " + type.name());
        return alert;
    }
}
