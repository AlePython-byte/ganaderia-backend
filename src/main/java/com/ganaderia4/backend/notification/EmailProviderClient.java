package com.ganaderia4.backend.notification;

public interface EmailProviderClient {

    String getProviderName();

    void send(EmailNotificationRequest request);
}
