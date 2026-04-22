package com.ganaderia4.backend.security;

import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class JpaDeviceReplayProtectionStore implements DeviceReplayProtectionStore {

    private final DeviceReplayNonceRepository deviceReplayNonceRepository;

    public JpaDeviceReplayProtectionStore(DeviceReplayNonceRepository deviceReplayNonceRepository) {
        this.deviceReplayNonceRepository = deviceReplayNonceRepository;
    }

    @Override
    @Transactional
    public boolean registerNonce(String deviceToken, String nonce, Instant expiresAt) {
        Instant now = Instant.now();

        deviceReplayNonceRepository.deleteExpiredNonces(now);

        int insertedRows = deviceReplayNonceRepository.insertNonce(
                deviceToken,
                nonce,
                expiresAt,
                now
        );

        return insertedRows == 1;
    }
}
