package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.observability.DomainMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DefaultNotificationDispatcherTest {

    @Test
    void shouldDispatchNotificationToAllRegisteredServices() {
        NotificationService serviceA = mock(NotificationService.class);
        NotificationService serviceB = mock(NotificationService.class);
        when(serviceA.getChannel()).thenReturn(NotificationChannel.LOG);
        when(serviceB.getChannel()).thenReturn(NotificationChannel.LOG);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(
                        List.of(serviceA, serviceB),
                        new DomainMetricsService(meterRegistry)
                );

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
        assertEquals(
                2.0,
                meterRegistry.counter(
                        "ganaderia.notifications.sent",
                        "channel", "LOG",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
    }

    @Test
    void shouldContinueDispatchingWhenOneServiceFails() {
        NotificationService failingService = mock(NotificationService.class);
        NotificationService successfulService = mock(NotificationService.class);

        when(failingService.getChannel()).thenReturn(NotificationChannel.LOG);
        when(successfulService.getChannel()).thenReturn(NotificationChannel.LOG);
        doThrow(new RuntimeException("fallo simulado"))
                .when(failingService)
                .send(any(NotificationMessage.class));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(
                        List.of(failingService, successfulService),
                        new DomainMetricsService(meterRegistry)
                );

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Mensaje de prueba")
                .severity("HIGH")
                .metadata("alertType", "COLLAR_OFFLINE")
                .build();

        dispatcher.dispatch(message);

        verify(failingService).send(message);
        verify(successfulService).send(message);
        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.failed",
                        "channel", "LOG",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.sent",
                        "channel", "LOG",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
    }

    @Test
    void shouldDoNothingWhenMessageIsNull() {
        NotificationService service = mock(NotificationService.class);

        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(
                        List.of(service),
                        new DomainMetricsService(new SimpleMeterRegistry())
                );

        dispatcher.dispatch(null);

        verify(service, never()).send(any());
    }

    @Test
    void shouldDoNothingWhenThereAreNoNotificationServices() {
        DefaultNotificationDispatcher dispatcher =
                new DefaultNotificationDispatcher(
                        List.of(),
                        new DomainMetricsService(new SimpleMeterRegistry())
                );

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta crítica")
                .message("Mensaje de prueba")
                .severity("HIGH")
                .build();

        dispatcher.dispatch(message);
    }
}
