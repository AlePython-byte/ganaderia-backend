package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CollarRepository extends JpaRepository<Collar, Long> {

    Optional<Collar> findByToken(String token);

    List<Collar> findByStatus(CollarStatus status);

    Optional<Collar> findByCow(Cow cow);

    List<Collar> findByEnabledTrueAndLastSeenAtBefore(LocalDateTime threshold);
}