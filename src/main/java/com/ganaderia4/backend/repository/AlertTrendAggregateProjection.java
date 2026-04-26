package com.ganaderia4.backend.repository;

import java.time.LocalDate;

public interface AlertTrendAggregateProjection {

    LocalDate getDate();

    long getTotalAlerts();

    long getPendingAlerts();

    long getResolvedAlerts();

    long getDiscardedAlerts();
}
