package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.WebhookNotificationDeliveryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class WebhookNotificationDeliveryProcessorTest {

    @Test
    void shouldMarkDeliveryAsDeliveredWhenWebhookReturns2xx() throws Exception {
        WebhookNotificationDelivery delivery = pendingDelivery();
        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookDeliveryClient deliveryClient = mock(WebhookDeliveryClient.class);
        when(repository.findEligibleForProcessing(anyList(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(delivery));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(repository.save(any(WebhookNotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryClient.send(any(WebhookNotificationDelivery.class), eq(1)))
                .thenReturn(new WebhookDeliveryResponse(202));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        WebhookNotificationDeliveryProcessor processor = new WebhookNotificationDeliveryProcessor(
                repository,
                deliveryClient,
                new DomainMetricsService(meterRegistry),
                webhookProperties(),
                new NoOpTransactionManager()
        );

        processor.processDueDeliveries();

        assertEquals(WebhookNotificationDeliveryStatus.DELIVERED, delivery.getStatus());
        assertEquals(1, delivery.getAttempts());
        assertNull(delivery.getLastError());
        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.sent",
                        "channel", "WEBHOOK",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
    }

    @Test
    void shouldLogSanitizedWebhookHostAndProcessorSummary(CapturedOutput output) throws Exception {
        WebhookNotificationDelivery delivery = pendingDelivery();
        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookDeliveryClient deliveryClient = mock(WebhookDeliveryClient.class);
        when(repository.findEligibleForProcessing(anyList(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(delivery));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(repository.save(any(WebhookNotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryClient.send(any(WebhookNotificationDelivery.class), eq(1)))
                .thenReturn(new WebhookDeliveryResponse(202));

        WebhookNotificationDeliveryProcessor processor = new WebhookNotificationDeliveryProcessor(
                repository,
                deliveryClient,
                new DomainMetricsService(new SimpleMeterRegistry()),
                webhookProperties(),
                new NoOpTransactionManager()
        );

        processor.processDueDeliveries();

        String logs = output.getOut();
        assertTrue(logs.contains("event=webhook_delivery_success"));
        assertTrue(logs.contains("host=https://example.com"));
        assertTrue(logs.contains("status=202"));
        assertTrue(logs.contains("notificationType=CRITICAL_ALERT_CREATED"));
        assertTrue(logs.contains("event=webhook_delivery_processor_completed"));
        assertTrue(logs.contains("requestId=scheduled"));
        assertFalse(logs.contains("/notifications"));
        assertFalse(logs.contains("access_token=secret-token"));
    }

    @Test
    void shouldScheduleRetryWhenWebhookReturnsRetryableStatus() throws Exception {
        WebhookNotificationDelivery delivery = pendingDelivery();
        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookDeliveryClient deliveryClient = mock(WebhookDeliveryClient.class);
        when(repository.findEligibleForProcessing(anyList(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(delivery));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(repository.save(any(WebhookNotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryClient.send(any(WebhookNotificationDelivery.class), eq(1)))
                .thenReturn(new WebhookDeliveryResponse(503));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        WebhookNotificationDeliveryProcessor processor = new WebhookNotificationDeliveryProcessor(
                repository,
                deliveryClient,
                new DomainMetricsService(meterRegistry),
                webhookProperties(),
                new NoOpTransactionManager()
        );

        processor.processDueDeliveries();

        assertEquals(WebhookNotificationDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(1, delivery.getAttempts());
        assertEquals("HTTP_503", delivery.getLastError());
        assertTrue(delivery.getNextAttemptAt().isAfter(LocalDateTime.now().minusSeconds(1)));
        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.failed",
                        "channel", "WEBHOOK",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.retried",
                        "channel", "WEBHOOK",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
    }

    @Test
    void shouldMarkDeliveryAsPermanentFailureWhenWebhookReturnsNonRetryableStatus() throws Exception {
        WebhookNotificationDelivery delivery = pendingDelivery();
        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookDeliveryClient deliveryClient = mock(WebhookDeliveryClient.class);
        when(repository.findEligibleForProcessing(anyList(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(delivery));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(repository.save(any(WebhookNotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryClient.send(any(WebhookNotificationDelivery.class), eq(1)))
                .thenReturn(new WebhookDeliveryResponse(400));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        WebhookNotificationDeliveryProcessor processor = new WebhookNotificationDeliveryProcessor(
                repository,
                deliveryClient,
                new DomainMetricsService(meterRegistry),
                webhookProperties(),
                new NoOpTransactionManager()
        );

        processor.processDueDeliveries();

        assertEquals(WebhookNotificationDeliveryStatus.FAILED_PERMANENT, delivery.getStatus());
        assertEquals(1, delivery.getAttempts());
        assertEquals("HTTP_400", delivery.getLastError());
        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.failed",
                        "channel", "WEBHOOK",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );
    }

    @Test
    void shouldMarkDeliveryAsPermanentFailureWhenRetryableExceptionExhaustsAttempts() throws Exception {
        WebhookNotificationDelivery delivery = pendingDelivery();
        delivery.setAttempts(2);

        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookDeliveryClient deliveryClient = mock(WebhookDeliveryClient.class);
        when(repository.findEligibleForProcessing(anyList(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(delivery));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(repository.save(any(WebhookNotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryClient.send(any(WebhookNotificationDelivery.class), eq(3)))
                .thenThrow(new IOException("network down"));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        WebhookNotificationDeliveryProcessor processor = new WebhookNotificationDeliveryProcessor(
                repository,
                deliveryClient,
                new DomainMetricsService(meterRegistry),
                webhookProperties(),
                new NoOpTransactionManager()
        );

        processor.processDueDeliveries();

        assertEquals(WebhookNotificationDeliveryStatus.FAILED_PERMANENT, delivery.getStatus());
        assertEquals(3, delivery.getAttempts());
        assertEquals("IO_EXCEPTION", delivery.getLastError());
    }

    private WebhookNotificationDelivery pendingDelivery() {
        WebhookNotificationDelivery delivery = WebhookNotificationDelivery.pending(
                "CRITICAL_ALERT_CREATED",
                "{\"eventType\":\"CRITICAL_ALERT_CREATED\"}",
                "https://example.com/notifications?access_token=secret-token"
        );
        delivery.setId(1L);
        return delivery;
    }

    private WebhookNotificationProperties webhookProperties() {
        WebhookNotificationProperties properties = new WebhookNotificationProperties();
        properties.setEnabled(true);
        properties.setUrl("https://example.com/notifications?access_token=secret-token");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setProcessorEnabled(true);
        properties.setProcessorFixedDelay(Duration.ofSeconds(15));
        properties.setProcessorBatchSize(20);
        properties.setMaxAttempts(3);
        properties.setRetryBackoff(Duration.ofSeconds(30));
        return properties;
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
