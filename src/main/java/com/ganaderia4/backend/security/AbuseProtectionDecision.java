package com.ganaderia4.backend.security;

public record AbuseProtectionDecision(boolean allowed, long retryAfterSeconds) {

    public static AbuseProtectionDecision allow() {
        return new AbuseProtectionDecision(true, 0);
    }

    public static AbuseProtectionDecision blocked(long retryAfterSeconds) {
        return new AbuseProtectionDecision(false, Math.max(1, retryAfterSeconds));
    }
}
