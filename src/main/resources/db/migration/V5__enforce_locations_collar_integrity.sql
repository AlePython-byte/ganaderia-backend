ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS collar_id BIGINT;

UPDATE locations l
SET collar_id = c.id
    FROM collars c
WHERE l.collar_id IS NULL
  AND c.cow_id = l.cow_id;

DO $$
DECLARE
missing_count BIGINT;
BEGIN
SELECT COUNT(*)
INTO missing_count
FROM locations
WHERE collar_id IS NULL;

IF missing_count > 0 THEN
        RAISE EXCEPTION 'No se puede forzar NOT NULL en locations.collar_id: % registros siguen con collar_id NULL', missing_count;
END IF;
END
$$;

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

ALTER TABLE locations
    ALTER COLUMN collar_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_locations_collar_timestamp
    ON locations (collar_id, timestamp DESC);