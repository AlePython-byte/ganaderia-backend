package com.ganaderia4.backend.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingNotificationService implements NotificationService {

    private final List<NotificationMessage> sentMessages = new ArrayList<>();

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.LOG;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {
        if (notificationMessage != null) {
            sentMessages.add(notificationMessage);
        }
    }

    public List<NotificationMessage> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
    }
}