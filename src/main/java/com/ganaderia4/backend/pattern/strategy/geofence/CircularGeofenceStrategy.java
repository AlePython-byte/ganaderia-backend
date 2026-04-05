package com.ganaderia4.backend.pattern.strategy.geofence;

import com.ganaderia4.backend.model.Geofence;
import org.springframework.stereotype.Component;

@Component
public class CircularGeofenceStrategy implements GeofenceEvaluationStrategy {

    @Override
    public boolean supports(Geofence geofence) {
        return geofence != null
                && geofence.getCenterLatitude() != null
                && geofence.getCenterLongitude() != null
                && geofence.getRadiusMeters() != null;
    }

    @Override
    public boolean isInside(Double latitude, Double longitude, Geofence geofence) {
        double distance = calculateDistanceMeters(
                latitude,
                longitude,
                geofence.getCenterLatitude(),
                geofence.getCenterLongitude()
        );

        return distance <= geofence.getRadiusMeters();
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadius = 6371000;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}