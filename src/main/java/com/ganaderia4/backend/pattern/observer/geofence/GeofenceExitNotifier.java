package com.ganaderia4.backend.pattern.observer.geofence;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeofenceExitNotifier {

    private final List<GeofenceExitObserver> observers;

    public GeofenceExitNotifier(List<GeofenceExitObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    public void notifyExit(GeofenceExitEvent event) {
        for (GeofenceExitObserver observer : observers) {
            observer.onGeofenceExit(event);
        }
    }
}
