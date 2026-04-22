CREATE TABLE IF NOT EXISTS abuse_rate_limits (
    id BIGSERIAL PRIMARY KEY,
    scope VARCHAR(50) NOT NULL,
    abuse_key VARCHAR(256) NOT NULL,
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL,
    blocked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_abuse_rate_limits_scope_key UNIQUE (scope, abuse_key)
);

CREATE INDEX IF NOT EXISTS idx_abuse_rate_limits_blocked_until
    ON abuse_rate_limits (blocked_until);

CREATE INDEX IF NOT EXISTS idx_abuse_rate_limits_updated_at
    ON abuse_rate_limits (updated_at);
