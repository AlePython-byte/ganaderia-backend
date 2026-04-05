package com.ganaderia4.backend.pattern.strategy.geofence;

import com.ganaderia4.backend.model.Geofence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeofenceStrategyResolver {

    private final List<GeofenceEvaluationStrategy> strategies;

    public GeofenceStrategyResolver(List<GeofenceEvaluationStrategy> strategies) {
        this.strategies = strategies;
    }

    public GeofenceEvaluationStrategy resolve(Geofence geofence) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(geofence))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe una estrategia válida para la geocerca"));
    }
}