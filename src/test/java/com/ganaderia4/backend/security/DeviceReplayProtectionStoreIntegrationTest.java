package com.ganaderia4.backend.security;

import com.ganaderia4.backend.model.DeviceReplayNonce;
import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceReplayProtectionStoreIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceReplayProtectionStore deviceReplayProtectionStore;

    @Autowired
    private DeviceReplayNonceRepository deviceReplayNonceRepository;

    @BeforeEach
    void setUp() {
        deviceReplayNonceRepository.deleteAll();
    }

    @Test
    void shouldRejectPreviouslyRegisteredNonce() {
        Instant expiresAt = Instant.now().plusSeconds(300);

        assertTrue(deviceReplayProtectionStore.registerNonce("COLLAR-STORE-001", "nonce-001", expiresAt));
        assertFalse(deviceReplayProtectionStore.registerNonce("COLLAR-STORE-001", "nonce-001", expiresAt));

        assertEquals(1, deviceReplayNonceRepository.count());
    }

    @Test
    void shouldCleanupExpiredNonceBeforeRegisteringSameNonce() {
        DeviceReplayNonce expiredNonce = new DeviceReplayNonce(
                "COLLAR-STORE-002",
                "nonce-002",
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(20)
        );
        deviceReplayNonceRepository.save(expiredNonce);

        assertTrue(deviceReplayProtectionStore.registerNonce(
                "COLLAR-STORE-002",
                "nonce-002",
                Instant.now().plusSeconds(300)
        ));

        assertEquals(1, deviceReplayNonceRepository.count());
    }
}
