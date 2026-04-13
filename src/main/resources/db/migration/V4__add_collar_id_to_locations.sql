ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS collar_id BIGINT;

UPDATE locations l
SET collar_id = c.id
    FROM collars c
WHERE c.cow_id = l.cow_id
  AND l.collar_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_locations_collar'
    ) THEN
ALTER TABLE locations
    ADD CONSTRAINT fk_locations_collar
        FOREIGN KEY (collar_id) REFERENCES collars(id);
END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_locations_collar_timestamp
    ON locations (collar_id, timestamp DESC);