package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.notification.WebhookNotificationDelivery;
import com.ganaderia4.backend.notification.WebhookNotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebhookNotificationDeliveryRepository extends JpaRepository<WebhookNotificationDelivery, Long> {

    Optional<WebhookNotificationDelivery> findByNotificationId(String notificationId);

    long countByStatus(WebhookNotificationDeliveryStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select delivery
            from WebhookNotificationDelivery delivery
            where delivery.status in :statuses
              and delivery.nextAttemptAt <= :nextAttemptAt
            order by delivery.nextAttemptAt asc, delivery.id asc
            """)
    List<WebhookNotificationDelivery> findEligibleForProcessing(@Param("statuses") List<WebhookNotificationDeliveryStatus> statuses,
                                                                @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                                                                Pageable pageable);
}
