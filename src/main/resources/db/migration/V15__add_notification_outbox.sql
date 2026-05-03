CREATE TABLE IF NOT EXISTS notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    recipient VARCHAR(255),
    subject VARCHAR(255),
    payload TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    next_attempt_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    failed_at TIMESTAMP NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_status_next_attempt
    ON notification_outbox (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_created_at
    ON notification_outbox (created_at);
