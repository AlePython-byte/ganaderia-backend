package com.ganaderia4.backend.notification;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private final List<NotificationService> notificationServices;

    public DefaultNotificationDispatcher(List<NotificationService> notificationServices) {
        this.notificationServices = notificationServices;
    }

    @Override
    public void dispatch(NotificationMessage notificationMessage) {
        if (notificationMessage == null || notificationServices == null || notificationServices.isEmpty()) {
            return;
        }

        for (NotificationService notificationService : notificationServices) {
            notificationService.send(notificationMessage);
        }
    }
}