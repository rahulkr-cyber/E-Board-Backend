-- =====================================================================
-- V10: Audit tables (immutable audit log)
-- =====================================================================

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY,
    user_id       UUID,
    username      VARCHAR(100),
    module        VARCHAR(100),
    entity_type   VARCHAR(100),
    entity_id     UUID,
    action        VARCHAR(100) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    ip_address    VARCHAR(100),
    machine_name  VARCHAR(200),
    browser       TEXT,
    api_path      TEXT,
    http_method   VARCHAR(20),
    success       BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_logs_user       ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action     ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Audit logs are immutable: revoke UPDATE/DELETE at application level;
-- optionally enforce with a trigger.
CREATE OR REPLACE FUNCTION fn_audit_logs_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is immutable: % not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION fn_audit_logs_immutable();
