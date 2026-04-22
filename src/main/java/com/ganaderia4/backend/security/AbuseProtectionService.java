package com.ganaderia4.backend.security;

public interface AbuseProtectionService {

    AbuseProtectionDecision checkAllowed(String scope, String key, AbuseProtectionPolicy policy);

    AbuseProtectionDecision recordFailure(String scope, String key, AbuseProtectionPolicy policy);

    void reset(String scope, String key);
}
