package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CowRepository extends JpaRepository<Cow, Long> {

    Optional<Cow> findByIdentifier(String identifier);

    Optional<Cow> findByInternalCode(String internalCode);

    List<Cow> findByStatus(String status);
}