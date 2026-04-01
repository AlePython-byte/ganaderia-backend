package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Page<Location> findByCowOrderByTimestampDesc(Cow cow, Pageable pageable);

    Page<Location> findByCowAndTimestampBetweenOrderByTimestampDesc(Cow cow, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Optional<Location> findTopByCowOrderByTimestampDesc(Cow cow);
}