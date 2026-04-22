package com.ganaderia4.backend.security;

import java.util.Optional;

public interface DeviceSigningSecretService {

    Optional<String> resolveSigningSecret(String deviceToken);
}
