package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CowRepository extends JpaRepository<Cow, Long> {

    Optional<Cow> findByToken(String token);

    Optional<Cow> findByInternalCode(String internalCode);

    List<Cow> findByStatus(CowStatus status);
}