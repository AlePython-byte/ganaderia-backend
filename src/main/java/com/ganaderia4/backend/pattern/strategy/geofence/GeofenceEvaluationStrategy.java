package com.ganaderia4.backend.pattern.strategy.geofence;

import com.ganaderia4.backend.model.Geofence;

public interface GeofenceEvaluationStrategy {

    boolean supports(Geofence geofence);

    boolean isInside(Double latitude, Double longitude, Geofence geofence);
}