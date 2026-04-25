CREATE TABLE IF NOT EXISTS webhook_notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    destination VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_webhook_notification_deliveries_notification_id UNIQUE (notification_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_notification_deliveries_status_next_attempt
    ON webhook_notification_deliveries (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_webhook_notification_deliveries_created_at
    ON webhook_notification_deliveries (created_at);
