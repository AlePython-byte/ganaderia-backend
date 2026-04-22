package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Geofence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    List<Geofence> findByActive(Boolean active);

    Page<Geofence> findByActive(Boolean active, Pageable pageable);

    Optional<Geofence> findByCowAndActive(Cow cow, Boolean active);
}
