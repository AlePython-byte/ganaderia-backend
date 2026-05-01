package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.GpsAccuracyQuality;
import org.springframework.stereotype.Component;

@Component
public final class GpsAccuracyClassifier {

    private static final double GOOD_MAX_METERS = 10.0;
    private static final double MODERATE_MAX_METERS = 30.0;

    public GpsAccuracyQuality classify(Double gpsAccuracy) {
        if (gpsAccuracy == null) {
            return GpsAccuracyQuality.UNKNOWN;
        }

        if (gpsAccuracy <= GOOD_MAX_METERS) {
            return GpsAccuracyQuality.GOOD;
        }

        if (gpsAccuracy <= MODERATE_MAX_METERS) {
            return GpsAccuracyQuality.MODERATE;
        }

        return GpsAccuracyQuality.LOW;
    }
}
