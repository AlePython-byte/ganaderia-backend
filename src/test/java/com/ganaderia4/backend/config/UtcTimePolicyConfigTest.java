package com.ganaderia4.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UtcTimePolicyConfigTest {

    @Test
    void shouldExposeUtcClockBean() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TimeConfig.class)) {
            Clock clock = context.getBean(Clock.class);

            assertNotNull(clock);
            assertEquals(ZoneOffset.UTC, clock.getZone());
        }
    }

    @Test
    void shouldConfigureJacksonWithUtcWithoutChangingLocalDateTimeContract() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(JacksonConfig.class)) {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

            assertNotNull(objectMapper);
            assertEquals("UTC", objectMapper.getSerializationConfig().getTimeZone().getID());
            assertEquals(
                    "\"2026-04-27T15:30:00\"",
                    objectMapper.writeValueAsString(LocalDateTime.of(2026, 4, 27, 15, 30, 0))
            );
        }
    }
}
