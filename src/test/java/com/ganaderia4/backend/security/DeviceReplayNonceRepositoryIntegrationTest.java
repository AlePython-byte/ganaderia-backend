package com.ganaderia4.backend.security;

import com.ganaderia4.backend.model.DeviceReplayNonce;
import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceReplayNonceRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceReplayNonceRepository deviceReplayNonceRepository;

    @BeforeEach
    void setUp() {
        deviceReplayNonceRepository.deleteAll();
    }

    @Test
    @Transactional
    void shouldDeleteOnlyNoncesOlderThanCutoff() {
        deviceReplayNonceRepository.save(new DeviceReplayNonce(
                "COLLAR-CLEANUP-001",
                "nonce-old",
                Instant.parse("2026-05-02T17:05:00Z"),
                Instant.parse("2026-05-02T17:00:00Z")
        ));
        deviceReplayNonceRepository.save(new DeviceReplayNonce(
                "COLLAR-CLEANUP-002",
                "nonce-recent",
                Instant.parse("2026-05-02T17:45:00Z"),
                Instant.parse("2026-05-02T17:40:00Z")
        ));

        int deleted = deviceReplayNonceRepository.deleteByCreatedAtLessThanEqual(
                Instant.parse("2026-05-02T17:30:00Z")
        );

        assertEquals(1, deleted);
        assertEquals(1, deviceReplayNonceRepository.count());
    }
}
