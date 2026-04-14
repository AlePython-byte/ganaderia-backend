package com.ganaderia4.backend.notification;

public interface NotificationDispatcher {

    void dispatch(NotificationMessage notificationMessage);
}