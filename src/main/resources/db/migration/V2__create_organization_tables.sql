-- =====================================================================
-- V2: Organization tables (departments, sections, designations,
--     postings, transfers, joining/relieving, charge assignments)
-- =====================================================================

CREATE TABLE org_departments (
    id          UUID PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE org_sections (
    id            UUID PRIMARY KEY,
    department_id UUID NOT NULL,
    code          VARCHAR(50) UNIQUE NOT NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    active        BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    created_by    UUID,
    updated_by    UUID,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_org_sections_department ON org_sections(department_id);

CREATE TABLE org_designations (
    id              UUID PRIMARY KEY,
    code            VARCHAR(50) UNIQUE NOT NULL,
    name            VARCHAR(200) NOT NULL,
    hierarchy_level INT NOT NULL,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE org_user_postings (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL,
    department_id      UUID NOT NULL,
    section_id         UUID NOT NULL,
    designation_id     UUID NOT NULL,
    posting_start_date DATE NOT NULL,
    posting_end_date   DATE,
    order_number       VARCHAR(100),
    order_date         DATE,
    remarks            TEXT,
    active             BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP,
    created_by         UUID,
    updated_by         UUID
);

CREATE INDEX idx_org_user_postings_user ON org_user_postings(user_id);

CREATE TABLE org_transfer_history (
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

CREATE INDEX idx_org_transfer_history_user ON org_transfer_history(user_id);

CREATE TABLE org_joining_relieving (
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

CREATE INDEX idx_org_joining_relieving_user ON org_joining_relieving(user_id);

CREATE TABLE org_charge_assignments (
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

CREATE INDEX idx_org_charge_from_user ON org_charge_assignments(from_user_id);
CREATE INDEX idx_org_charge_to_user   ON org_charge_assignments(to_user_id);
CREATE INDEX idx_org_charge_status    ON org_charge_assignments(status);
