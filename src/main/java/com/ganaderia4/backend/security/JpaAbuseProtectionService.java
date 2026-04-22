package com.ganaderia4.backend.security;

import com.ganaderia4.backend.model.AbuseRateLimitEntry;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class JpaAbuseProtectionService implements AbuseProtectionService {

    private final AbuseRateLimitRepository abuseRateLimitRepository;

    public JpaAbuseProtectionService(AbuseRateLimitRepository abuseRateLimitRepository) {
        this.abuseRateLimitRepository = abuseRateLimitRepository;
    }

    @Override
    @Transactional
    public AbuseProtectionDecision checkAllowed(String scope, String key, AbuseProtectionPolicy policy) {
        Instant now = Instant.now();
        return abuseRateLimitRepository.findByScopeAndAbuseKey(scope, key)
                .map(entry -> decisionFromBlock(entry.getBlockedUntil(), now))
                .orElseGet(AbuseProtectionDecision::allow);
    }

    @Override
    @Transactional
    public AbuseProtectionDecision recordFailure(String scope, String key, AbuseProtectionPolicy policy) {
        return recordAttempt(scope, key, policy);
    }

    @Override
    @Transactional
    public AbuseProtectionDecision recordAttempt(String scope, String key, AbuseProtectionPolicy policy) {
        Instant now = Instant.now();
        abuseRateLimitRepository.insertIfMissing(scope, key, now);

        AbuseRateLimitEntry entry = abuseRateLimitRepository.findByScopeAndAbuseKey(scope, key)
                .orElseThrow(() -> new IllegalStateException("Abuse rate limit entry could not be loaded"));

        AbuseProtectionDecision existingBlock = decisionFromBlock(entry.getBlockedUntil(), now);
        if (!existingBlock.allowed()) {
            return existingBlock;
        }

        Instant windowResetBefore = now.minus(validWindow(policy));
        if (entry.getWindowStart() == null || !entry.getWindowStart().isAfter(windowResetBefore)) {
            entry.setWindowStart(now);
            entry.setAttemptCount(1);
            entry.setBlockedUntil(null);
        } else {
            entry.setAttemptCount(entry.getAttemptCount() + 1);
        }

        if (entry.getAttemptCount() > validMaxAttempts(policy)) {
            Instant blockedUntil = now.plus(validBlockDuration(policy));
            entry.setBlockedUntil(blockedUntil);
            entry.setUpdatedAt(now);
            abuseRateLimitRepository.save(entry);
            return AbuseProtectionDecision.blocked(secondsUntil(blockedUntil, now));
        }

        entry.setUpdatedAt(now);
        abuseRateLimitRepository.save(entry);
        return AbuseProtectionDecision.allow();
    }

    @Override
    @Transactional
    public void reset(String scope, String key) {
        abuseRateLimitRepository.deleteByScopeAndAbuseKey(scope, key);
    }

    private AbuseProtectionDecision decisionFromBlock(Instant blockedUntil, Instant now) {
        if (blockedUntil == null || !blockedUntil.isAfter(now)) {
            return AbuseProtectionDecision.allow();
        }

        return AbuseProtectionDecision.blocked(secondsUntil(blockedUntil, now));
    }

    private long secondsUntil(Instant instant, Instant now) {
        return Math.max(1, Duration.between(now, instant).toSeconds());
    }

    private Duration validWindow(AbuseProtectionPolicy policy) {
        if (policy.window() == null || policy.window().isZero() || policy.window().isNegative()) {
            return Duration.ofMinutes(15);
        }

        return policy.window();
    }

    private int validMaxAttempts(AbuseProtectionPolicy policy) {
        return Math.max(1, policy.maxAttempts());
    }

    private Duration validBlockDuration(AbuseProtectionPolicy policy) {
        if (policy.blockDuration() == null
                || policy.blockDuration().isZero()
                || policy.blockDuration().isNegative()) {
            return Duration.ofMinutes(15);
        }

        return policy.blockDuration();
    }
}
