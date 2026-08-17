-- =====================================================================
-- V28: Configurable DMS document types.
-- This table is owned exclusively by the DMS module and does not reuse or
-- modify master_document_types or any attachment-related table.
-- =====================================================================

CREATE TABLE dms_document_types (
    id          UUID PRIMARY KEY,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_dms_document_types_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uq_dms_document_types_code
    ON dms_document_types (LOWER(code))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uq_dms_document_types_name
    ON dms_document_types (LOWER(name))
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_document_types_active_sort
    ON dms_document_types (active, sort_order, name)
    WHERE deleted = FALSE;
