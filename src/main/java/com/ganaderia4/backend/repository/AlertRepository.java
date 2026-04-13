package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByType(AlertType type);

    Optional<Alert> findByCowAndTypeAndStatus(Cow cow, AlertType type, AlertStatus status);

    List<Alert> findTop10ByStatusOrderByCreatedAtDesc(AlertStatus status);

    long countByStatus(AlertStatus status);

    long countByTypeAndStatus(AlertType type, AlertStatus status);
}