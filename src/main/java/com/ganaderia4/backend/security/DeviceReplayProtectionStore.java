package com.ganaderia4.backend.security;

import java.time.Instant;

public interface DeviceReplayProtectionStore {

    boolean registerNonce(String deviceToken, String nonce, Instant expiresAt);
}
