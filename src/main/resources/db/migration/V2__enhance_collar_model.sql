ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS battery_level INTEGER;

ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP;

ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS signal_status VARCHAR(50);

ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS firmware_version VARCHAR(100);

ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS gps_accuracy DOUBLE PRECISION;

ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE INDEX IF NOT EXISTS idx_collars_last_seen_at
    ON collars (last_seen_at);

CREATE INDEX IF NOT EXISTS idx_collars_signal_status
    ON collars (signal_status);