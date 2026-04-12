CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL
    );

CREATE TABLE IF NOT EXISTS cows (
                                    id BIGSERIAL PRIMARY KEY,
                                    identifier VARCHAR(255) NOT NULL UNIQUE,
    internal_code VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    observations TEXT
    );

CREATE TABLE IF NOT EXISTS collars (
                                       id BIGSERIAL PRIMARY KEY,
                                       identifier VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    cow_id BIGINT UNIQUE,
    CONSTRAINT fk_collars_cow
    FOREIGN KEY (cow_id) REFERENCES cows(id)
    );

CREATE TABLE IF NOT EXISTS geofences (
                                         id BIGSERIAL PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
    center_latitude DOUBLE PRECISION NOT NULL,
    center_longitude DOUBLE PRECISION NOT NULL,
    radius_meters DOUBLE PRECISION NOT NULL,
    active BOOLEAN NOT NULL,
    cow_id BIGINT,
    CONSTRAINT fk_geofences_cow
    FOREIGN KEY (cow_id) REFERENCES cows(id)
    );

CREATE TABLE IF NOT EXISTS locations (
                                         id BIGSERIAL PRIMARY KEY,
                                         latitude DOUBLE PRECISION NOT NULL,
                                         longitude DOUBLE PRECISION NOT NULL,
                                         timestamp TIMESTAMP NOT NULL,
                                         cow_id BIGINT NOT NULL,
                                         collar_id BIGINT NOT NULL,
                                         CONSTRAINT fk_locations_cow
                                         FOREIGN KEY (cow_id) REFERENCES cows(id),
    CONSTRAINT fk_locations_collar
    FOREIGN KEY (collar_id) REFERENCES collars(id)
    );

CREATE TABLE IF NOT EXISTS alerts (
                                      id BIGSERIAL PRIMARY KEY,
                                      type VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    observations TEXT,
    cow_id BIGINT NOT NULL,
    location_id BIGINT,
    CONSTRAINT fk_alerts_cow
    FOREIGN KEY (cow_id) REFERENCES cows(id),
    CONSTRAINT fk_alerts_location
    FOREIGN KEY (location_id) REFERENCES locations(id)
    );

CREATE INDEX IF NOT EXISTS idx_locations_cow_timestamp
    ON locations (cow_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_locations_collar_timestamp
    ON locations (collar_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_status
    ON alerts (status);

CREATE INDEX IF NOT EXISTS idx_alerts_type
    ON alerts (type);

CREATE INDEX IF NOT EXISTS idx_geofences_active
    ON geofences (active);

CREATE UNIQUE INDEX IF NOT EXISTS uq_geofences_active_cow
    ON geofences (cow_id)
    WHERE active = TRUE AND cow_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_alerts_pending_by_cow_and_type
    ON alerts (cow_id, type)
    WHERE status = 'PENDIENTE';