CREATE INDEX IF NOT EXISTS idx_alerts_created_at_desc
    ON alerts (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_status_created_at_desc
    ON alerts (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_cow_created_at_id_desc
    ON alerts (cow_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_locations_timestamp_desc
    ON locations (timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_collars_enabled_signal_last_seen_at
    ON collars (signal_status, last_seen_at ASC)
    WHERE enabled = TRUE;
