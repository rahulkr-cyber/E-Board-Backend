-- =====================================================================
-- V15: Charge, transfer, and joining/relieving tables
-- =====================================================================

CREATE TABLE IF NOT EXISTS org_charge_assignments (
    id             UUID PRIMARY KEY,
    from_user_id   UUID NOT NULL,
    to_user_id     UUID NOT NULL,
    charge_type    VARCHAR(50) NOT NULL,
    department_id  UUID,
    section_id     UUID,
    effective_from TIMESTAMP NOT NULL,
    effective_to   TIMESTAMP,
    order_number   VARCHAR(100),
    order_date     DATE,
    approved_by    UUID,
    attachment_id  UUID,
    reason         TEXT,
    status         VARCHAR(30) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,
    created_by     UUID,
    updated_by     UUID,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_charge_to_user
    ON org_charge_assignments(to_user_id, status);

CREATE INDEX IF NOT EXISTS idx_charge_from_user
    ON org_charge_assignments(from_user_id, status);

CREATE INDEX IF NOT EXISTS idx_charge_status
    ON org_charge_assignments(status);


CREATE TABLE IF NOT EXISTS org_transfer_history (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    from_department_id  UUID,
    from_section_id     UUID,
    to_department_id    UUID NOT NULL,
    to_section_id       UUID NOT NULL,
    from_designation_id UUID,
    to_designation_id   UUID,
    transfer_date       DATE NOT NULL,
    order_number        VARCHAR(100),
    order_date          DATE,
    attachment_id       UUID,
    remarks             TEXT,
    created_at          TIMESTAMP NOT NULL,
    created_by          UUID
);

CREATE INDEX IF NOT EXISTS idx_transfer_user
    ON org_transfer_history(user_id);


CREATE TABLE IF NOT EXISTS org_joining_relieving (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    event_type     VARCHAR(30) NOT NULL,
    department_id  UUID,
    section_id     UUID,
    designation_id UUID,
    event_date     DATE NOT NULL,
    order_number   VARCHAR(100),
    order_date     DATE,
    attachment_id  UUID,
    remarks        TEXT,
    created_at     TIMESTAMP NOT NULL,
    created_by     UUID
);

CREATE INDEX IF NOT EXISTS idx_joining_user
    ON org_joining_relieving(user_id, event_date);
