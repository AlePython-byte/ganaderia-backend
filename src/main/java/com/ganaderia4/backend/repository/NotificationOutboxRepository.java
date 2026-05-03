package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationOutboxMessage;
import com.ganaderia4.backend.notification.NotificationOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxMessage, Long> {

    long countByStatus(NotificationOutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message
            from NotificationOutboxMessage message
            where message.channel = :channel
              and message.status in :statuses
              and message.nextAttemptAt <= :nextAttemptAt
              and message.attempts < message.maxAttempts
            order by message.nextAttemptAt asc, message.id asc
            """)
    List<NotificationOutboxMessage> findEligibleForProcessing(@Param("channel") NotificationChannel channel,
                                                              @Param("statuses") List<NotificationOutboxStatus> statuses,
                                                              @Param("nextAttemptAt") Instant nextAttemptAt,
                                                              Pageable pageable);
}
