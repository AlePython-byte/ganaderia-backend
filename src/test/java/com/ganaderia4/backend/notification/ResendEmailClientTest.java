package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResendEmailClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendExpectedRequestToResendEndpoint() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> contentTypeHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        startServer(exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentTypeHeader.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
        });

        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(true);
        properties.setProvider("resend");
        properties.setApiKey("resend-api-key");
        properties.setFrom("alerts@ganaderia.test");
        properties.setTo("ops@ganaderia.test");
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(2000);
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());

        ResendEmailClient client = new ResendEmailClient(properties, objectMapper);
        client.send(new EmailNotificationRequest(
                "alerts@ganaderia.test",
                "ops@ganaderia.test",
                "[Ganaderia 4.0] Alerta operativa",
                "Mensaje de prueba"
        ));

        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertEquals("Bearer resend-api-key", authorizationHeader.get());
        assertEquals("application/json", contentTypeHeader.get());
        assertEquals("alerts@ganaderia.test", payload.get("from").asText());
        assertEquals("ops@ganaderia.test", payload.get("to").asText());
        assertEquals("[Ganaderia 4.0] Alerta operativa", payload.get("subject").asText());
        assertEquals("Mensaje de prueba", payload.get("text").asText());
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
