CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGSERIAL PRIMARY KEY,
                                          action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    actor VARCHAR(255),
    source VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at
    ON audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action
    ON audit_logs (action);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type
    ON audit_logs (entity_type);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor
    ON audit_logs (actor);