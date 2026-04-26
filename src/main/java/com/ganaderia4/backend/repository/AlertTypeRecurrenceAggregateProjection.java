package com.ganaderia4.backend.repository;

import java.time.LocalDateTime;

public interface AlertTypeRecurrenceAggregateProjection {

    String getType();

    long getTotalAlerts();

    long getPendingAlerts();

    long getResolvedAlerts();

    long getDiscardedAlerts();

    LocalDateTime getLastAlertAt();
}
