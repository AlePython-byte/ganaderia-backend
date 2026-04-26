package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AlertPriorityScorer {

    private final AlertRepository alertRepository;
    private final CollarRepository collarRepository;

    @Value("${app.device-monitor.offline-threshold-minutes:15}")
    private long offlineThresholdMinutes;

    public AlertPriorityScorer(AlertRepository alertRepository,
                               CollarRepository collarRepository) {
        this.alertRepository = alertRepository;
        this.collarRepository = collarRepository;
    }

    public AlertPriorityAssessment score(Alert alert) {
        if (!isOperationallyScorable(alert)) {
            return AlertPriorityAssessment.none();
        }

        int score = baseTypeScore(alert.getType())
                + alertAgeScore(alert.getCreatedAt())
                + recurrenceScore(alert.getCow())
                + activeConditionScore(alert);

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

    private int alertAgeScore(LocalDateTime createdAt) {
        if (createdAt == null || createdAt.isAfter(LocalDateTime.now())) {
            return 0;
        }

        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
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

    private int recurrenceScore(Cow cow) {
        long incidents = alertRepository.countByCow(cow);

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

    private int activeConditionScore(Alert alert) {
        if (alert.getType() == AlertType.EXIT_GEOFENCE) {
            return alert.getCow().getStatus() == CowStatus.FUERA ? 20 : 0;
        }

        if (alert.getType() == AlertType.COLLAR_OFFLINE) {
            return offlineDurationScore(alert.getCow());
        }

        return 0;
    }

    private int offlineDurationScore(Cow cow) {
        Collar collar = collarRepository.findByCow(cow).orElse(null);
        if (collar == null) {
            return 0;
        }

        LocalDateTime lastSeenAt = collar.getLastSeenAt();
        if (lastSeenAt == null) {
            return 20;
        }

        if (lastSeenAt.isAfter(LocalDateTime.now())) {
            return 0;
        }

        long minutesWithoutReport = Duration.between(lastSeenAt, LocalDateTime.now()).toMinutes();
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
}
