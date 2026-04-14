package com.ganaderia4.backend.notification;

public interface NotificationService {

    NotificationChannel getChannel();

    void send(NotificationMessage notificationMessage);
}