package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    List<Geofence> findByActive(Boolean active);

    Optional<Geofence> findByCowAndActive(Cow cow, Boolean active);
}