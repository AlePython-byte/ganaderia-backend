package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollarRepository extends JpaRepository<Collar, Long> {

    Optional<Collar> findByIdentifier(String identifier);

    List<Collar> findByStatus(String status);

    Optional<Collar> findByCow(Cow cow);
}