package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class WebhookNotificationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private HttpServer server;

    @AfterEach
    void tearDown() {
        MDC.clear();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldPostNotificationPayloadToWebhook(CapturedOutput output) throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> eventHeader = new AtomicReference<>();
        AtomicReference<String> signatureHeader = new AtomicReference<>();

        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            eventHeader.set(exchange.getRequestHeaders().getFirst("X-Ganaderia4-Event-Type"));
            signatureHeader.set(exchange.getRequestHeaders().getFirst("X-Ganaderia4-Signature"));
            exchange.sendResponseHeaders(202, -1);
        });

        WebhookNotificationProperties properties = webhookProperties("webhook-secret");
        WebhookNotificationService service = new WebhookNotificationService(properties, objectMapper);
        MDC.put("requestId", "req-webhook-001");

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Collar sin senal")
                .severity("HIGH")
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 30))
                .metadata("alertType", "COLLAR_OFFLINE")
                .metadata("cowToken", "VACA-001")
                .build();

        service.send(message);

        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertEquals("CRITICAL_ALERT_CREATED", payload.get("eventType").asText());
        assertEquals("Nueva alerta critica", payload.get("title").asText());
        assertEquals("HIGH", payload.get("severity").asText());
        assertEquals("COLLAR_OFFLINE", payload.get("metadata").get("alertType").asText());
        assertEquals("CRITICAL_ALERT_CREATED", eventHeader.get());
        assertEquals("sha256=" + sign(requestBody.get(), "webhook-secret"), signatureHeader.get());

        String logs = output.getOut();
        assertTrue(logs.contains("event=notification_webhook_result"));
        assertTrue(logs.contains("channel=WEBHOOK"));
        assertTrue(logs.contains("result=success"));
        assertTrue(logs.contains("requestId=req-webhook-001"));
        assertTrue(logs.contains("destination=http://localhost:"));
        assertTrue(logs.contains("status=202"));
        assertTrue(logs.contains("durationMs="));
        assertTrue(logs.contains("eventType=CRITICAL_ALERT_CREATED"));
        assertFalse(logs.contains("webhook-secret"));
        assertFalse(logs.contains(requestBody.get()));
    }

    @Test
    void shouldThrowWhenWebhookReturnsErrorStatus(CapturedOutput output) throws Exception {
        startServer(exchange -> exchange.sendResponseHeaders(500, -1));

        WebhookNotificationProperties properties = webhookProperties("");
        WebhookNotificationService service = new WebhookNotificationService(properties, objectMapper);

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Collar sin senal")
                .severity("HIGH")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.send(message));
        assertEquals("Webhook notification failed with status 500", ex.getMessage());

        String logs = output.getOut();
        assertTrue(logs.contains("event=notification_webhook_result"));
        assertTrue(logs.contains("result=failure"));
        assertTrue(logs.contains("status=500"));
        assertTrue(logs.contains("destination=http://localhost:"));
    }

    @Test
    void shouldIgnoreNullMessage() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(202, -1);
        });

        WebhookNotificationProperties properties = webhookProperties("");
        WebhookNotificationService service = new WebhookNotificationService(properties, objectMapper);

        service.send(null);

        assertEquals(0, requestCount.get());
    }

    @Test
    void shouldRejectEnabledWebhookWithoutUrl() {
        WebhookNotificationProperties properties = new WebhookNotificationProperties();
        properties.setUrl("");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new WebhookNotificationService(properties, objectMapper)
        );

        assertEquals("Webhook notification URL must be configured when webhook channel is enabled", ex.getMessage());
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/notifications", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private WebhookNotificationProperties webhookProperties(String secret) {
        WebhookNotificationProperties properties = new WebhookNotificationProperties();
        properties.setEnabled(true);
        properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/notifications");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setSecret(secret);
        return properties;
    }

    private String sign(String requestBody, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8)));
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
