package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.notification.NotificationOutboxMessage;
import com.ganaderia4.backend.notification.NotificationOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxMessage, Long> {

    long countByStatus(NotificationOutboxStatus status);
}
