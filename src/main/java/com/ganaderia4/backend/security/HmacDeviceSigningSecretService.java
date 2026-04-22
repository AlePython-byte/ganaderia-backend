package com.ganaderia4.backend.security;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class HmacDeviceSigningSecretService implements DeviceSigningSecretService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final CollarRepository collarRepository;
    private final String masterKey;

    public HmacDeviceSigningSecretService(CollarRepository collarRepository,
                                          @Value("${device.auth.secret-master-key:}") String masterKey) {
        this.collarRepository = collarRepository;
        this.masterKey = masterKey == null ? "" : masterKey;
    }

    @PostConstruct
    void validateConfiguration() {
        if (masterKey.isBlank()) {
            throw new IllegalStateException("device.auth.secret-master-key must be configured");
        }

        if (masterKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("device.auth.secret-master-key must be at least 32 bytes");
        }
    }

    @Override
    public Optional<String> resolveSigningSecret(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank() || masterKey.isBlank()) {
            return Optional.empty();
        }

        return collarRepository.findByToken(deviceToken.trim())
                .map(Collar::getDeviceSecretSalt)
                .filter(salt -> salt != null && !salt.isBlank())
                .map(salt -> deriveSecret(deviceToken.trim(), salt));
    }

    private String deriveSecret(String deviceToken, String salt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(masterKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] secretBytes = mac.doFinal((deviceToken + "\n" + salt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        } catch (Exception ex) {
            return "";
        }
    }
}
