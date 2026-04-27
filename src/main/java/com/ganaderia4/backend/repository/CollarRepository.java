package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CollarRepository extends JpaRepository<Collar, Long> {

    Optional<Collar> findByToken(String token);

    List<Collar> findByStatus(CollarStatus status);

    Page<Collar> findByStatus(CollarStatus status, Pageable pageable);

    List<Collar> findByEnabledTrueAndSignalStatusOrderByLastSeenAtAsc(DeviceSignalStatus signalStatus);

    Optional<Collar> findByCow(Cow cow);

    List<Collar> findByCowIdIn(Collection<Long> cowIds);

    List<Collar> findByEnabledTrueAndStatusAndLastSeenAtBefore(CollarStatus status, LocalDateTime threshold);

    List<Collar> findByEnabledTrueAndSignalStatus(DeviceSignalStatus signalStatus);

    long countByEnabledTrue();

    long countByEnabledTrueAndLastSeenAtIsNull();

    long countByEnabledTrueAndLastSeenAtBefore(LocalDateTime threshold);

    long countByEnabledTrueAndLastSeenAtGreaterThanEqual(LocalDateTime threshold);

    long countByStatus(CollarStatus status);

    long countByEnabledTrueAndSignalStatus(DeviceSignalStatus signalStatus);
}
