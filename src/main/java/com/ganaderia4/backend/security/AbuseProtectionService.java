package com.ganaderia4.backend.security;

public interface AbuseProtectionService {

    AbuseProtectionDecision checkAllowed(String scope, String key, AbuseProtectionPolicy policy);

    AbuseProtectionDecision recordAttempt(String scope, String key, AbuseProtectionPolicy policy);

    default AbuseProtectionDecision recordFailure(String scope, String key, AbuseProtectionPolicy policy) {
        return recordAttempt(scope, key, policy);
    }

    void reset(String scope, String key);
}
