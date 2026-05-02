package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.DeviceReplayNonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface DeviceReplayNonceRepository extends JpaRepository<DeviceReplayNonce, Long> {

    @Modifying
    @Query("DELETE FROM DeviceReplayNonce nonce WHERE nonce.expiresAt <= :now")
    int deleteExpiredNonces(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM DeviceReplayNonce nonce WHERE nonce.createdAt <= :cutoff")
    int deleteByCreatedAtLessThanEqual(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query(
            value = """
                    INSERT INTO device_replay_nonces (device_token, nonce, expires_at, created_at)
                    VALUES (:deviceToken, :nonce, :expiresAt, :createdAt)
                    ON CONFLICT (device_token, nonce) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertNonce(@Param("deviceToken") String deviceToken,
                    @Param("nonce") String nonce,
                    @Param("expiresAt") Instant expiresAt,
                    @Param("createdAt") Instant createdAt);
}
