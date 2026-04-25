package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpWebhookDeliveryClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendPersistedPayloadWithIdempotencyHeaders() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> notificationIdHeader = new AtomicReference<>();
        AtomicReference<String> idempotencyKeyHeader = new AtomicReference<>();
        AtomicReference<String> attemptHeader = new AtomicReference<>();
        AtomicReference<String> signatureHeader = new AtomicReference<>();

        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            notificationIdHeader.set(exchange.getRequestHeaders().getFirst("X-Ganaderia4-Notification-Id"));
            idempotencyKeyHeader.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            attemptHeader.set(exchange.getRequestHeaders().getFirst("X-Ganaderia4-Delivery-Attempt"));
            signatureHeader.set(exchange.getRequestHeaders().getFirst("X-Ganaderia4-Signature"));
            exchange.sendResponseHeaders(202, -1);
        });

        WebhookNotificationProperties properties = new WebhookNotificationProperties();
        properties.setEnabled(true);
        properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/notifications");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setSecret("webhook-secret");

        HttpWebhookDeliveryClient client = new HttpWebhookDeliveryClient(properties);

        WebhookNotificationDelivery delivery = WebhookNotificationDelivery.pending(
                "CRITICAL_ALERT_CREATED",
                "{\"eventType\":\"CRITICAL_ALERT_CREATED\",\"title\":\"Nueva alerta\"}",
                "http://localhost:" + server.getAddress().getPort() + "/notifications"
        );
        delivery.setNotificationId("notif-001-abc");

        WebhookDeliveryResponse response = client.send(delivery, 2);

        assertEquals(202, response.statusCode());
        assertEquals(delivery.getPayload(), requestBody.get());
        assertEquals("notif-001-abc", notificationIdHeader.get());
        assertEquals("notif-001-abc", idempotencyKeyHeader.get());
        assertEquals("2", attemptHeader.get());
        assertEquals("sha256=" + sign(delivery.getPayload(), "webhook-secret"), signatureHeader.get());
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
