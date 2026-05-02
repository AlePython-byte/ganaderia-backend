package com.ganaderia4.backend.notification;

public interface NotificationService {

    NotificationChannel getChannel();

    NotificationSendResult send(NotificationMessage notificationMessage);
}
