package com.ganaderia4.backend.security;

import java.time.Duration;

public record AbuseProtectionPolicy(Duration window, int maxAttempts, Duration blockDuration) {
}
