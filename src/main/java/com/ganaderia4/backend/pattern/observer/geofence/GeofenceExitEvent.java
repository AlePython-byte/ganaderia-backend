package com.ganaderia4.backend.pattern.observer.geofence;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;

public class GeofenceExitEvent {

    private final Cow cow;
    private final Location location;

    public GeofenceExitEvent(Cow cow, Location location) {
        this.cow = cow;
        this.location = location;
    }

    public Cow getCow() {
        return cow;
    }

    public Location getLocation() {
        return location;
    }
}