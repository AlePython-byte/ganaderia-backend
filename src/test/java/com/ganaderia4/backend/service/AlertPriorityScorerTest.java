package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertPriorityScorerTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CollarRepository collarRepository;

    private AlertPriorityScorer alertPriorityScorer;

    @BeforeEach
    void setUp() {
        alertPriorityScorer = new AlertPriorityScorer(alertRepository, collarRepository, Clock.systemDefaultZone());
    }

    @Test
    void shouldAssignHighPriorityToRecurringPendingExitGeofenceAlertStillOutside() {
        Cow cow = new Cow();
        cow.setId(1L);
        cow.setToken("VACA-001");
        cow.setStatus(CowStatus.FUERA);

        Alert alert = new Alert();
        alert.setId(10L);
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCreatedAt(LocalDateTime.now().minusHours(2));
        alert.setCow(cow);

        when(alertRepository.countByCow(cow)).thenReturn(6L);

        AlertPriorityAssessment assessment = alertPriorityScorer.score(alert);

        assertEquals(90, assessment.priorityScore());
        assertEquals("HIGH", assessment.priority());
    }

    @Test
    void shouldAssignMediumPriorityToPendingCollarOfflineAlertUsingLastSeenAt() {
        ReflectionTestUtils.setField(alertPriorityScorer, "offlineThresholdMinutes", 15L);

        Cow cow = new Cow();
        cow.setId(2L);
        cow.setToken("VACA-002");
        cow.setStatus(CowStatus.DENTRO);

        Alert alert = new Alert();
        alert.setId(11L);
        alert.setType(AlertType.COLLAR_OFFLINE);
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        alert.setCow(cow);

        Collar collar = new Collar();
        collar.setId(20L);
        collar.setCow(cow);
        collar.setLastSeenAt(LocalDateTime.now().minusMinutes(20));

        when(alertRepository.countByCow(cow)).thenReturn(1L);
        when(collarRepository.findByCow(cow)).thenReturn(Optional.of(collar));

        AlertPriorityAssessment assessment = alertPriorityScorer.score(alert);

        assertEquals(50, assessment.priorityScore());
        assertEquals("MEDIUM", assessment.priority());
    }

    @Test
    void shouldReturnNoPriorityForResolvedAlert() {
        Cow cow = new Cow();
        cow.setId(3L);

        Alert alert = new Alert();
        alert.setType(AlertType.EXIT_GEOFENCE);
        alert.setStatus(AlertStatus.RESUELTA);
        alert.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        alert.setCow(cow);

        AlertPriorityAssessment assessment = alertPriorityScorer.score(alert);

        assertNull(assessment.priorityScore());
        assertNull(assessment.priority());
    }

    @Test
    void shouldBuildBatchContextAndReuseItForPendingAlerts() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-25T12:00:00Z"), ZoneId.of("UTC"));
        AlertPriorityScorer scorer = new AlertPriorityScorer(alertRepository, collarRepository, fixedClock);
        ReflectionTestUtils.setField(scorer, "offlineThresholdMinutes", 15L);

        Cow cow = new Cow();
        cow.setId(4L);
        cow.setToken("VACA-004");
        cow.setStatus(CowStatus.DENTRO);

        Alert alert = new Alert();
        alert.setId(12L);
        alert.setType(AlertType.COLLAR_OFFLINE);
        alert.setStatus(AlertStatus.PENDIENTE);
        alert.setCreatedAt(LocalDateTime.of(2026, 4, 25, 11, 30));
        alert.setCow(cow);

        Collar collar = new Collar();
        collar.setId(21L);
        collar.setCow(cow);
        collar.setLastSeenAt(LocalDateTime.of(2026, 4, 25, 11, 20));

        when(alertRepository.countGroupedByCowIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{4L, 5L}));
        when(collarRepository.findByCowIdIn(anyCollection())).thenReturn(List.of(collar));

        AlertPriorityScorer.AlertPriorityScoringContext context = scorer.buildContext(List.of(alert));
        AlertPriorityAssessment assessment = scorer.score(alert, context);

        assertEquals(Map.of(4L, 5L), context.incidentCountByCowId());
        assertEquals(60, assessment.priorityScore());
        assertEquals("MEDIUM", assessment.priority());
    }
}
