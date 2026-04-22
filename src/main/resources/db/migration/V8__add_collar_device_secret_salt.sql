ALTER TABLE collars
    ADD COLUMN IF NOT EXISTS device_secret_salt VARCHAR(100);

UPDATE collars
SET device_secret_salt =
        md5(identifier || ':' || id || ':' || random()::text || ':' || clock_timestamp()::text)
        || md5(id::text || ':' || random()::text || ':' || clock_timestamp()::text)
WHERE device_secret_salt IS NULL OR btrim(device_secret_salt) = '';

ALTER TABLE collars
    ALTER COLUMN device_secret_salt SET NOT NULL;
