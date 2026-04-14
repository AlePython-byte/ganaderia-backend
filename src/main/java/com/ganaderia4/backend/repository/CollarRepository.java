package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ganaderia4.backend.model.DeviceSignalStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CollarRepository extends JpaRepository<Collar, Long> {

    Optional<Collar> findByToken(String token);

    List<Collar> findByStatus(CollarStatus status);

    List<Collar> findByEnabledTrueAndSignalStatusOrderByLastSeenAtAsc(DeviceSignalStatus signalStatus);

    Optional<Collar> findByCow(Cow cow);

    List<Collar> findByEnabledTrueAndLastSeenAtBefore(LocalDateTime threshold);

    List<Collar> findByEnabledTrueAndSignalStatus(DeviceSignalStatus signalStatus);

    long countByStatus(CollarStatus status);

    long countByEnabledTrueAndSignalStatus(DeviceSignalStatus signalStatus);
}