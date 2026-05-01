package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertAnalysisSummaryDTO;
import com.ganaderia4.backend.dto.AlertPriorityRecommendationDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlertAnalysisService {

    static final String CONFIDENCE_RULE_BASED = "RULE_BASED";
    static final int DEFAULT_TOP_PRIORITIES_LIMIT = 5;
    static final int MAX_TOP_PRIORITIES_LIMIT = 20;

    private static final Logger log = LoggerFactory.getLogger(AlertAnalysisService.class);
    private static final int HIGH_PENDING_VOLUME_THRESHOLD = 10;
    private static final int HIGH_PRIORITY_PENDING_THRESHOLD = 3;
    private static final int MULTIPLE_LOW_BATTERY_THRESHOLD = 2;
    private static final int RECURRENT_PENDING_ALERT_THRESHOLD = 2;

    private final AlertRepository alertRepository;
    private final AlertPriorityScorer alertPriorityScorer;

    public AlertAnalysisService(AlertRepository alertRepository,
                                AlertPriorityScorer alertPriorityScorer) {
        this.alertRepository = alertRepository;
        this.alertPriorityScorer = alertPriorityScorer;
    }

    public AlertAnalysisSummaryDTO getSummary() {
        List<Alert> pendingAlerts = alertRepository.findByStatus(AlertStatus.PENDIENTE);
        List<Alert> recentPendingAlerts = pendingAlerts.isEmpty()
                ? List.of()
                : alertRepository.findTop10ByStatusOrderByCreatedAtDesc(AlertStatus.PENDIENTE);

        AlertHeuristicSnapshot snapshot = buildSnapshot(pendingAlerts, recentPendingAlerts);
        AlertAnalysisRiskLevel riskLevel = deriveRiskLevel(snapshot);
        List<String> criticalSignals = buildCriticalSignals(snapshot);
        List<String> recommendedActions = buildRecommendedActions(snapshot);

        AlertAnalysisSummaryDTO summary = new AlertAnalysisSummaryDTO(
                riskLevel,
                snapshot.totalPendingAlerts(),
                criticalSignals,
                recommendedActions,
                CONFIDENCE_RULE_BASED
        );

        log.info(
                "event=alert_analysis_summary_generated requestId={} riskLevel={} totalPendingAlerts={}",
                OperationalLogSanitizer.requestId(),
                riskLevel,
                snapshot.totalPendingAlerts()
        );

        return summary;
    }

    public List<AlertPriorityRecommendationDTO> getTopPriorities(Integer limit) {
        int resolvedLimit = resolveTopPrioritiesLimit(limit);
        List<Alert> pendingAlerts = alertRepository.findByStatus(AlertStatus.PENDIENTE);
        AlertPriorityScorer.AlertPriorityScoringContext scoringContext = alertPriorityScorer.buildContext(pendingAlerts);

        List<AlertPriorityRecommendationDTO> recommendations = pendingAlerts.stream()
                .map(alert -> toPriorityRecommendation(alert, alertPriorityScorer.score(alert, scoringContext)))
                .sorted((left, right) -> {
                    int byScore = Integer.compare(
                            right.getPriorityScore() != null ? right.getPriorityScore() : Integer.MIN_VALUE,
                            left.getPriorityScore() != null ? left.getPriorityScore() : Integer.MIN_VALUE
                    );
                    if (byScore != 0) {
                        return byScore;
                    }

                    if (left.getCreatedAt() != null && right.getCreatedAt() != null) {
                        int byCreatedAt = left.getCreatedAt().compareTo(right.getCreatedAt());
                        if (byCreatedAt != 0) {
                            return byCreatedAt;
                        }
                    }

                    if (left.getAlertId() == null || right.getAlertId() == null) {
                        return 0;
                    }

                    return Long.compare(left.getAlertId(), right.getAlertId());
                })
                .limit(resolvedLimit)
                .toList();

        log.info(
                "event=alert_analysis_top_priorities_generated requestId={} returned={} limit={}",
                OperationalLogSanitizer.requestId(),
                recommendations.size(),
                resolvedLimit
        );

        return recommendations;
    }

    private AlertHeuristicSnapshot buildSnapshot(List<Alert> pendingAlerts, List<Alert> recentPendingAlerts) {
        List<Alert> safePendingAlerts = pendingAlerts != null ? pendingAlerts : List.of();
        List<Alert> safeRecentPendingAlerts = recentPendingAlerts != null ? recentPendingAlerts : List.of();

        long offlinePendingCount = countByType(safePendingAlerts, AlertType.COLLAR_OFFLINE);
        long lowBatteryPendingCount = countByType(safePendingAlerts, AlertType.LOW_BATTERY);
        long exitGeofencePendingCount = countByType(safePendingAlerts, AlertType.EXIT_GEOFENCE);

        AlertPriorityScorer.AlertPriorityScoringContext scoringContext = alertPriorityScorer.buildContext(safePendingAlerts);
        long highPriorityPendingCount = safePendingAlerts.stream()
                .map(alert -> alertPriorityScorer.score(alert, scoringContext))
                .filter(assessment -> "HIGH".equals(assessment.priority()))
                .count();

        boolean hasRecurrentAlerts = safePendingAlerts.stream()
                .map(Alert::getCow)
                .filter(Objects::nonNull)
                .map(cow -> cow.getId())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
                .values()
                .stream()
                .anyMatch(count -> count >= RECURRENT_PENDING_ALERT_THRESHOLD);

        boolean hasRecentExitGeofence = safeRecentPendingAlerts.stream()
                .anyMatch(alert -> alert.getType() == AlertType.EXIT_GEOFENCE);

        return new AlertHeuristicSnapshot(
                safePendingAlerts.size(),
                offlinePendingCount,
                lowBatteryPendingCount,
                exitGeofencePendingCount,
                highPriorityPendingCount,
                hasRecurrentAlerts,
                hasRecentExitGeofence
        );
    }

    private AlertAnalysisRiskLevel deriveRiskLevel(AlertHeuristicSnapshot snapshot) {
        if (snapshot.totalPendingAlerts() == 0) {
            return AlertAnalysisRiskLevel.LOW;
        }

        if (snapshot.highPriorityPendingCount() >= HIGH_PRIORITY_PENDING_THRESHOLD
                || (snapshot.offlinePendingCount() > 0 && snapshot.lowBatteryPendingCount() > 0)
                || snapshot.totalPendingAlerts() >= HIGH_PENDING_VOLUME_THRESHOLD) {
            return AlertAnalysisRiskLevel.CRITICAL;
        }

        if (snapshot.offlinePendingCount() > 0
                || snapshot.exitGeofencePendingCount() > 0
                || snapshot.lowBatteryPendingCount() >= MULTIPLE_LOW_BATTERY_THRESHOLD) {
            return AlertAnalysisRiskLevel.HIGH;
        }

        return AlertAnalysisRiskLevel.MEDIUM;
    }

    private List<String> buildCriticalSignals(AlertHeuristicSnapshot snapshot) {
        List<String> signals = new ArrayList<>();

        if (snapshot.offlinePendingCount() > 0) {
            signals.add("Hay collares offline pendientes.");
        }

        if (snapshot.lowBatteryPendingCount() > 0) {
            signals.add("Hay alertas de batería baja pendientes.");
        }

        if (snapshot.hasRecentExitGeofence()) {
            signals.add("Hay salidas de geocerca recientes.");
        } else if (snapshot.exitGeofencePendingCount() > 0) {
            signals.add("Hay salidas de geocerca pendientes.");
        }

        if (snapshot.highPriorityPendingCount() >= HIGH_PRIORITY_PENDING_THRESHOLD) {
            signals.add("Hay múltiples alertas pendientes con prioridad alta.");
        }

        if (snapshot.hasRecurrentAlerts()) {
            signals.add("Hay animales con alertas recurrentes pendientes.");
        }

        if (snapshot.totalPendingAlerts() >= HIGH_PENDING_VOLUME_THRESHOLD) {
            signals.add("El volumen de alertas pendientes es alto.");
        }

        return signals;
    }

    private List<String> buildRecommendedActions(AlertHeuristicSnapshot snapshot) {
        Set<String> actions = new LinkedHashSet<>();

        if (snapshot.totalPendingAlerts() == 0) {
            actions.add("No hay acciones críticas pendientes en este momento.");
            return new ArrayList<>(actions);
        }

        if (snapshot.offlinePendingCount() > 0) {
            actions.add("Revisar conectividad y estado físico de los collares offline.");
        }

        if (snapshot.exitGeofencePendingCount() > 0) {
            actions.add("Verificar ubicación física de animales fuera de geocerca.");
        }

        if (snapshot.lowBatteryPendingCount() > 0) {
            actions.add("Programar recarga o reemplazo de batería en collares con batería baja.");
        }

        if (snapshot.hasRecurrentAlerts()) {
            actions.add("Priorizar animales con alertas recurrentes.");
        }

        if (snapshot.highPriorityPendingCount() >= HIGH_PRIORITY_PENDING_THRESHOLD
                || snapshot.totalPendingAlerts() >= HIGH_PENDING_VOLUME_THRESHOLD) {
            actions.add("Priorizar la atención usando la cola de alertas por prioridad.");
        }

        return new ArrayList<>(actions);
    }

    private long countByType(List<Alert> alerts, AlertType type) {
        return alerts.stream()
                .filter(alert -> alert.getType() == type)
                .count();
    }

    private int resolveTopPrioritiesLimit(Integer limit) {
        int resolvedLimit = limit != null ? limit : DEFAULT_TOP_PRIORITIES_LIMIT;
        if (resolvedLimit < 1 || resolvedLimit > MAX_TOP_PRIORITIES_LIMIT) {
            throw new BadRequestException("El limit debe estar entre 1 y 20");
        }

        return resolvedLimit;
    }

    private AlertPriorityRecommendationDTO toPriorityRecommendation(Alert alert, AlertPriorityAssessment assessment) {
        return new AlertPriorityRecommendationDTO(
                alert.getId(),
                alert.getType() != null ? alert.getType().name() : null,
                alert.getStatus() != null ? alert.getStatus().name() : null,
                assessment.priorityScore(),
                assessment.priority(),
                reasonFor(alert.getType()),
                recommendedActionFor(alert.getType()),
                alert.getCow() != null ? alert.getCow().getId() : null,
                alert.getCow() != null ? OperationalLogSanitizer.maskToken(alert.getCow().getToken()) : "UNKNOWN",
                alert.getCreatedAt()
        );
    }

    private String reasonFor(AlertType alertType) {
        if (alertType == AlertType.COLLAR_OFFLINE) {
            return "El collar no reporta actividad reciente y puede dejar sin monitoreo al animal.";
        }

        if (alertType == AlertType.LOW_BATTERY) {
            return "El collar presenta batería baja y podría dejar de reportar próximamente.";
        }

        if (alertType == AlertType.EXIT_GEOFENCE) {
            return "El animal tiene una alerta pendiente de salida de geocerca.";
        }

        return "La alerta requiere revisión operativa.";
    }

    private String recommendedActionFor(AlertType alertType) {
        if (alertType == AlertType.COLLAR_OFFLINE) {
            return "Revisar conectividad, batería y estado físico del collar.";
        }

        if (alertType == AlertType.LOW_BATTERY) {
            return "Programar recarga o reemplazo de batería del collar.";
        }

        if (alertType == AlertType.EXIT_GEOFENCE) {
            return "Verificar la ubicación física del animal y revisar la configuración de la geocerca.";
        }

        return "Revisar el detalle de la alerta en la cola de prioridades.";
    }

    private record AlertHeuristicSnapshot(
            long totalPendingAlerts,
            long offlinePendingCount,
            long lowBatteryPendingCount,
            long exitGeofencePendingCount,
            long highPriorityPendingCount,
            boolean hasRecurrentAlerts,
            boolean hasRecentExitGeofence
    ) {
    }
}
