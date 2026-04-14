package com.ganaderia4.backend.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class DefaultNotificationDispatcherTest {

    @Test
    void shouldDispatchNotificationToAllRegisteredServices() {
        NotificationService serviceA = mock(NotificationService.class);
        NotificationService serviceB = mock(NotificationService.class);

        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(List.of(serviceA, serviceB));

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta crítica")
                .message("Mensaje de prueba")
                .severity("HIGH")
                .metadata("alertType", "COLLAR_OFFLINE")
                .build();

        dispatcher.dispatch(message);

        verify(serviceA).send(message);
        verify(serviceB).send(message);
    }

    @Test
    void shouldDoNothingWhenMessageIsNull() {
        NotificationService service = mock(NotificationService.class);

        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(List.of(service));

        dispatcher.dispatch(null);

        verify(service, never()).send(any());
    }

    @Test
    void shouldDoNothingWhenThereAreNoNotificationServices() {
        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(List.of());

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta crítica")
                .message("Mensaje de prueba")
                .severity("HIGH")
                .build();

        dispatcher.dispatch(message);
    }
}