package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM PasswordResetToken token
             WHERE (token.expiresAt < :cutoff)
                OR (token.usedAt IS NOT NULL AND token.usedAt < :cutoff)
            """)
    int deleteStaleTokens(@Param("cutoff") Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken token
               SET token.usedAt = :usedAt
             WHERE token.user.id = :userId
               AND token.usedAt IS NULL
               AND token.expiresAt > :now
            """)
    int invalidateActiveTokensForUser(@Param("userId") Long userId,
                                      @Param("usedAt") Instant usedAt,
                                      @Param("now") Instant now);
}
