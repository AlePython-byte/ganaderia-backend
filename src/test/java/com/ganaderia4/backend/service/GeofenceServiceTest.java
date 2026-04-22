package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Geofence;
import com.ganaderia4.backend.config.PaginationProperties;
import com.ganaderia4.backend.pattern.strategy.geofence.GeofenceEvaluationStrategy;
import com.ganaderia4.backend.pattern.strategy.geofence.GeofenceStrategyResolver;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.GeofenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeofenceServiceTest {

    private GeofenceService geofenceService;

    @BeforeEach
    void setUp() {
        GeofenceEvaluationStrategy circularStrategy = new com.ganaderia4.backend.pattern.strategy.geofence.CircularGeofenceStrategy();
        GeofenceStrategyResolver resolver = new GeofenceStrategyResolver(List.of(circularStrategy));

        geofenceService = new GeofenceService(
                null,
                null,
                resolver,
                new PaginationService(new PaginationProperties())
        );
    }

    @Test
    void shouldReturnTrueWhenPointIsInsideCircularGeofence() {
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(1.0);
        geofence.setCenterLongitude(1.0);
        geofence.setRadiusMeters(200.0);

        boolean result = geofenceService.isInsideGeofence(1.0005, 1.0005, geofence);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenPointIsOutsideCircularGeofence() {
        Geofence geofence = new Geofence();
        geofence.setCenterLatitude(1.0);
        geofence.setCenterLongitude(1.0);
        geofence.setRadiusMeters(50.0);

        boolean result = geofenceService.isInsideGeofence(1.01, 1.01, geofence);

        assertFalse(result);
    }
}
