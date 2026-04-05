package com.ganaderia4.backend.pattern.observer.geofence;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.repository.CowRepository;
import org.springframework.stereotype.Component;

@Component
public class CowStatusOnGeofenceExitObserver implements GeofenceExitObserver {

    private final CowRepository cowRepository;

    public CowStatusOnGeofenceExitObserver(CowRepository cowRepository) {
        this.cowRepository = cowRepository;
    }

    @Override
    public void onGeofenceExit(GeofenceExitEvent event) {
        Cow cow = event.getCow();
        cow.setStatus(CowStatus.FUERA);
        cowRepository.save(cow);
    }
}