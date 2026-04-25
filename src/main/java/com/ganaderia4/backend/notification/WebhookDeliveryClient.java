package com.ganaderia4.backend.notification;

import java.io.IOException;

public interface WebhookDeliveryClient {

    WebhookDeliveryResponse send(WebhookNotificationDelivery delivery, int attemptNumber)
            throws IOException, InterruptedException;
}
