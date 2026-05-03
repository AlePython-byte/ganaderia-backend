package com.ganaderia4.backend.notification;

import java.util.Locale;

public enum EmailDeliveryMode {
    DIRECT,
    OUTBOX;

    public static EmailDeliveryMode fromConfigValue(String value) {
        if (value == null || value.isBlank()) {
            return DIRECT;
        }

        try {
            return EmailDeliveryMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DIRECT;
        }
    }
}
