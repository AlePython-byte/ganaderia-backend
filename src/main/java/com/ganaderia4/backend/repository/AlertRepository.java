package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(String status);

    List<Alert> findByType(String type);

    Optional<Alert> findByCowAndTypeAndStatus(Cow cow, String type, String status);
}