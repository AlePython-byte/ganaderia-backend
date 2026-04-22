CREATE TABLE IF NOT EXISTS device_replay_nonces (
    id BIGSERIAL PRIMARY KEY,
    device_token VARCHAR(100) NOT NULL,
    nonce VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_replay_nonces_device_nonce UNIQUE (device_token, nonce)
);

CREATE INDEX IF NOT EXISTS idx_device_replay_nonces_expires_at
    ON device_replay_nonces (expires_at);
