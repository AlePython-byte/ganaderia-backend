package com.ganaderia4.backend.security;

import com.ganaderia4.backend.model.AbuseRateLimitEntry;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbuseRateLimitRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AbuseRateLimitRepository abuseRateLimitRepository;

    @BeforeEach
    void setUp() {
        abuseRateLimitRepository.deleteAll();
    }

    @Test
    @Transactional
    void shouldDeleteOnlyOldEntriesThatAreNotCurrentlyBlocked() {
        abuseRateLimitRepository.save(entry(
                "LOGIN_EMAIL",
                "old-unblocked",
                Instant.parse("2026-05-01T17:00:00Z"),
                null
        ));
        abuseRateLimitRepository.save(entry(
                "LOGIN_EMAIL",
                "recent-unblocked",
                Instant.parse("2026-05-02T17:40:00Z"),
                null
        ));
        abuseRateLimitRepository.save(entry(
                "DEVICE_IP",
                "old-blocked",
                Instant.parse("2026-05-01T17:00:00Z"),
                Instant.parse("2026-05-02T19:00:00Z")
        ));

        int deleted = abuseRateLimitRepository.deleteInactiveEntries(
                Instant.parse("2026-05-01T18:00:00Z"),
                Instant.parse("2026-05-02T18:00:00Z")
        );

        assertEquals(1, deleted);
        assertEquals(2, abuseRateLimitRepository.count());
    }

    private AbuseRateLimitEntry entry(String scope, String abuseKey, Instant updatedAt, Instant blockedUntil) {
        AbuseRateLimitEntry entry = new AbuseRateLimitEntry();
        entry.setScope(scope);
        entry.setAbuseKey(abuseKey);
        entry.setWindowStart(updatedAt);
        entry.setAttemptCount(1);
        entry.setBlockedUntil(blockedUntil);
        entry.setCreatedAt(updatedAt.minusSeconds(60));
        entry.setUpdatedAt(updatedAt);
        return entry;
    }
}
