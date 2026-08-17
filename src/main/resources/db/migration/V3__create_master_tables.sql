-- =====================================================================
-- V3: Master tables (03_DATABASE.md section 6)
-- letter categories, priorities, document types, languages,
-- holidays, system settings
-- =====================================================================

CREATE TABLE master_letter_categories (
    id                           UUID PRIMARY KEY,
    code                         VARCHAR(50) UNIQUE NOT NULL,
    name                         VARCHAR(150) NOT NULL,
    description                  TEXT,
    default_workflow_template_id UUID,
    active                       BOOLEAN DEFAULT TRUE,
    created_at                   TIMESTAMP NOT NULL,
    updated_at                   TIMESTAMP,
    created_by                   UUID,
    updated_by                   UUID,
    deleted                      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE master_priorities (
    id         UUID PRIMARY KEY,
    code       VARCHAR(50) UNIQUE NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    sla_days   INT,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE master_document_types (
    id          UUID PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE master_languages (
    id         UUID PRIMARY KEY,
    code       VARCHAR(20) UNIQUE NOT NULL,
    name       VARCHAR(100) NOT NULL,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE master_holidays (
    id           UUID PRIMARY KEY,
    holiday_date DATE NOT NULL,
    name         VARCHAR(200) NOT NULL,
    holiday_type VARCHAR(50),
    active       BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP,
    created_by   UUID,
    updated_by   UUID,
    deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_master_holidays_date ON master_holidays(holiday_date);

CREATE TABLE master_system_settings (
    id            UUID PRIMARY KEY,
    setting_key   VARCHAR(150) UNIQUE NOT NULL,
    setting_value TEXT,
    description   TEXT,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    created_by    UUID,
    updated_by    UUID,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE
);
