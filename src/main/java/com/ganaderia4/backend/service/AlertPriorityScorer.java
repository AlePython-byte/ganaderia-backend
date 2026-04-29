package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlertPriorityScorer {

    private final AlertRepository alertRepository;
    private final CollarRepository collarRepository;
    private final Clock clock;

    @Value("${app.device-monitor.offline-threshold-minutes:15}")
    private long offlineThresholdMinutes;

    @Autowired
    public AlertPriorityScorer(AlertRepository alertRepository,
                               CollarRepository collarRepository,
                               Clock clock) {
        this.alertRepository = alertRepository;
        this.collarRepository = collarRepository;
        this.clock = clock;
    }

    AlertPriorityScorer(AlertRepository alertRepository,
                        CollarRepository collarRepository) {
        this(alertRepository, collarRepository, Clock.systemUTC());
    }

    public AlertPriorityAssessment score(Alert alert) {
        if (!isOperationallyScorable(alert)) {
            return AlertPriorityAssessment.none();
        }

        LocalDateTime evaluatedAt = LocalDateTime.now(clock);
        int score = baseTypeScore(alert.getType())
                + alertAgeScore(alert.getCreatedAt(), evaluatedAt)
                + recurrenceScore(fetchIncidentCount(alert.getCow()))
                + activeConditionScore(alert, fetchCollar(alert), evaluatedAt);

        return new AlertPriorityAssessment(score, derivePriority(score));
    }

    public AlertPriorityScoringContext buildContext(List<Alert> alerts) {
        LocalDateTime evaluatedAt = LocalDateTime.now(clock);
        if (alerts == null || alerts.isEmpty()) {
            return AlertPriorityScoringContext.empty(evaluatedAt);
        }

        Set<Long> scorableCowIds = alerts.stream()
                .filter(this::isOperationallyScorable)
                .map(alert -> alert.getCow().getId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (scorableCowIds.isEmpty()) {
            return AlertPriorityScoringContext.empty(evaluatedAt);
        }

        Map<Long, Long> incidentCountByCowId = new HashMap<>();
        for (Object[] row : alertRepository.countGroupedByCowIds(scorableCowIds)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }

            incidentCountByCowId.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        Set<Long> offlineCowIds = alerts.stream()
                .filter(this::isOperationallyScorable)
                .filter(alert -> alert.getType() == AlertType.COLLAR_OFFLINE)
                .map(alert -> alert.getCow().getId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, Collar> collarByCowId = offlineCowIds.isEmpty()
                ? Map.of()
                : collarRepository.findByCowIdIn(offlineCowIds).stream()
                .filter(collar -> collar.getCow() != null && collar.getCow().getId() != null)
                .collect(Collectors.toMap(collar -> collar.getCow().getId(), collar -> collar, (left, right) -> left));

        return new AlertPriorityScoringContext(evaluatedAt, incidentCountByCowId, collarByCowId);
    }

    public AlertPriorityAssessment score(Alert alert, AlertPriorityScoringContext context) {
        if (!isOperationallyScorable(alert)) {
            return AlertPriorityAssessment.none();
        }

        LocalDateTime evaluatedAt = context != null ? context.evaluatedAt() : LocalDateTime.now(clock);
        long incidentCount = context != null ? context.incidentCountFor(alert.getCow()) : fetchIncidentCount(alert.getCow());
        Collar collar = context != null ? context.collarFor(alert.getCow()) : fetchCollar(alert);

        int score = baseTypeScore(alert.getType())
                + alertAgeScore(alert.getCreatedAt(), evaluatedAt)
                + recurrenceScore(incidentCount)
                + activeConditionScore(alert, collar, evaluatedAt);

        return new AlertPriorityAssessment(score, derivePriority(score));
    }

    private boolean isOperationallyScorable(Alert alert) {
        return alert != null
                && alert.getStatus() == AlertStatus.PENDIENTE
                && alert.getType() != null
                && alert.getCow() != null;
    }

    private int baseTypeScore(AlertType type) {
        if (type == AlertType.EXIT_GEOFENCE) {
            return 50;
        }

        if (type == AlertType.COLLAR_OFFLINE) {
            return 40;
        }

        return 20;
    }

    private int alertAgeScore(LocalDateTime createdAt, LocalDateTime evaluatedAt) {
        if (createdAt == null || createdAt.isAfter(evaluatedAt)) {
            return 0;
        }

        long minutes = Duration.between(createdAt, evaluatedAt).toMinutes();
        if (minutes >= 360) {
            return 20;
        }

        if (minutes >= 60) {
            return 10;
        }

        if (minutes >= 15) {
            return 5;
        }

        return 0;
    }

    private int recurrenceScore(long incidents) {
        if (incidents >= 10) {
            return 15;
        }

        if (incidents >= 5) {
            return 10;
        }

        if (incidents >= 3) {
            return 5;
        }

        return 0;
    }

    private int activeConditionScore(Alert alert, Collar collar, LocalDateTime evaluatedAt) {
        if (alert.getType() == AlertType.EXIT_GEOFENCE) {
            return alert.getCow().getStatus() == CowStatus.FUERA ? 20 : 0;
        }

        if (alert.getType() == AlertType.COLLAR_OFFLINE) {
            return offlineDurationScore(collar, evaluatedAt);
        }

        return 0;
    }

    private int offlineDurationScore(Collar collar, LocalDateTime evaluatedAt) {
        if (collar == null) {
            return 0;
        }

        LocalDateTime lastSeenAt = collar.getLastSeenAt();
        if (lastSeenAt == null) {
            return 20;
        }

        if (lastSeenAt.isAfter(evaluatedAt)) {
            return 0;
        }

        long minutesWithoutReport = Duration.between(lastSeenAt, evaluatedAt).toMinutes();
        if (minutesWithoutReport >= 360) {
            return 20;
        }

        if (minutesWithoutReport >= 60) {
            return 10;
        }

        if (minutesWithoutReport >= offlineThresholdMinutes) {
            return 5;
        }

        return 0;
    }

    private String derivePriority(int score) {
        if (score >= 70) {
            return "HIGH";
        }

        if (score >= 45) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private long fetchIncidentCount(Cow cow) {
        return alertRepository.countByCow(cow);
    }

    private Collar fetchCollar(Alert alert) {
        return alert.getCow() != null ? collarRepository.findByCow(alert.getCow()).orElse(null) : null;
    }

    public record AlertPriorityScoringContext(LocalDateTime evaluatedAt,
                                              Map<Long, Long> incidentCountByCowId,
                                              Map<Long, Collar> collarByCowId) {

        public static AlertPriorityScoringContext empty(LocalDateTime evaluatedAt) {
            return new AlertPriorityScoringContext(evaluatedAt, Map.of(), Map.of());
        }

        public long incidentCountFor(Cow cow) {
            if (cow == null || cow.getId() == null) {
                return 0;
            }

            return incidentCountByCowId.getOrDefault(cow.getId(), 0L);
        }

        public Collar collarFor(Cow cow) {
            if (cow == null || cow.getId() == null) {
                return null;
            }

            return collarByCowId.get(cow.getId());
        }
    }
}
