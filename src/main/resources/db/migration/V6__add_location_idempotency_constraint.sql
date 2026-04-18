CREATE UNIQUE INDEX IF NOT EXISTS uq_locations_collar_timestamp_coordinates
    ON locations (collar_id, timestamp, latitude, longitude);
