package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByCowOrderByTimestampAsc(Cow cow);

    List<Location> findByCowAndTimestampBetweenOrderByTimestampAsc(Cow cow, LocalDateTime start, LocalDateTime end);

    Optional<Location> findTopByCowOrderByTimestampDesc(Cow cow);
}