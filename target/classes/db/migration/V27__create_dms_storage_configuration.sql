-- =====================================================================
-- V27: Configurable storage registry owned exclusively by the DMS module.
-- Existing attachment storage tables and configuration are not modified.
-- =====================================================================

CREATE TABLE dms_storage_configuration (
    id                   UUID PRIMARY KEY,
    provider_code        VARCHAR(50) NOT NULL,
    display_name         VARCHAR(150) NOT NULL,
    base_path            VARCHAR(1000),
    configuration_json   JSONB NOT NULL DEFAULT '{}'::jsonb,
    primary_provider     BOOLEAN NOT NULL DEFAULT FALSE,
    health_check_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP,
    created_by           UUID,
    updated_by           UUID,
    deleted              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_dms_storage_provider_code
    ON dms_storage_configuration (LOWER(provider_code))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uq_dms_storage_primary_provider
    ON dms_storage_configuration (primary_provider)
    WHERE primary_provider = TRUE
      AND active = TRUE
      AND deleted = FALSE;

INSERT INTO dms_storage_configuration (
    id,
    provider_code,
    display_name,
    base_path,
    configuration_json,
    primary_provider,
    health_check_enabled,
    active,
    created_at,
    deleted
) VALUES (
    '70000000-0000-0000-0000-000000000001',
    'local',
    'Local DMS Storage',
    NULL,
    '{}'::jsonb,
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    FALSE
);
