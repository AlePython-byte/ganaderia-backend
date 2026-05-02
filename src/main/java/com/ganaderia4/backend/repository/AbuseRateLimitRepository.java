package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.AbuseRateLimitEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AbuseRateLimitRepository extends JpaRepository<AbuseRateLimitEntry, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AbuseRateLimitEntry> findByScopeAndAbuseKey(String scope, String abuseKey);

    @Modifying
    @Query(
            value = """
                    INSERT INTO abuse_rate_limits
                        (scope, abuse_key, window_start, attempt_count, blocked_until, created_at, updated_at)
                    VALUES
                        (:scope, :abuseKey, :now, 0, NULL, :now, :now)
                    ON CONFLICT (scope, abuse_key) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfMissing(@Param("scope") String scope,
                        @Param("abuseKey") String abuseKey,
                        @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM AbuseRateLimitEntry entry WHERE entry.scope = :scope AND entry.abuseKey = :abuseKey")
    int deleteByScopeAndAbuseKey(@Param("scope") String scope,
                                 @Param("abuseKey") String abuseKey);

    @Modifying
    @Query("""
            DELETE FROM AbuseRateLimitEntry entry
            WHERE entry.updatedAt <= :cutoff
              AND (entry.blockedUntil IS NULL OR entry.blockedUntil <= :now)
            """)
    int deleteInactiveEntries(@Param("cutoff") Instant cutoff,
                              @Param("now") Instant now);
}
