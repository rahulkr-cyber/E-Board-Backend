-- Configurable master data engine owned exclusively by the DMS module.
-- Existing business master tables and attachment tables are not changed.

CREATE TABLE dms_master_sources (
    id                  UUID PRIMARY KEY,
    code                VARCHAR(60) NOT NULL,
    name                VARCHAR(150) NOT NULL,
    description         VARCHAR(2000),
    source_type         VARCHAR(30) NOT NULL,
    value_field         VARCHAR(100) NOT NULL,
    label_field         VARCHAR(100) NOT NULL,
    response_path       VARCHAR(500),
    query_text          VARCHAR(10000),
    procedure_name      VARCHAR(200),
    endpoint_url        VARCHAR(2000),
    http_method         VARCHAR(10),
    configuration_json  JSONB NOT NULL DEFAULT '{}'::jsonb,
    cache_ttl_seconds   INTEGER NOT NULL DEFAULT 0,
    max_results         INTEGER NOT NULL DEFAULT 500,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_dms_master_source_cache_ttl
        CHECK (cache_ttl_seconds BETWEEN 0 AND 86400),
    CONSTRAINT ck_dms_master_source_max_results
        CHECK (max_results BETWEEN 1 AND 1000)
);

CREATE UNIQUE INDEX uq_dms_master_sources_code
    ON dms_master_sources (LOWER(code))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uq_dms_master_sources_name
    ON dms_master_sources (LOWER(name))
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_master_sources_active_name
    ON dms_master_sources (active, name)
    WHERE deleted = FALSE;

CREATE TABLE dms_master_source_parameters (
    id                  UUID PRIMARY KEY,
    master_source_id    UUID NOT NULL,
    parameter_name      VARCHAR(80) NOT NULL,
    target_name         VARCHAR(120) NOT NULL,
    parameter_location  VARCHAR(20) NOT NULL,
    data_type           VARCHAR(20) NOT NULL,
    required            BOOLEAN NOT NULL DEFAULT FALSE,
    default_value       VARCHAR(2000),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_master_parameter_source
        FOREIGN KEY (master_source_id) REFERENCES dms_master_sources (id),
    CONSTRAINT ck_dms_master_parameter_sort_order
        CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uq_dms_master_parameter_name
    ON dms_master_source_parameters (master_source_id, LOWER(parameter_name))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uq_dms_master_parameter_target
    ON dms_master_source_parameters (
        master_source_id,
        parameter_location,
        LOWER(target_name)
    )
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_master_parameter_order
    ON dms_master_source_parameters (
        master_source_id,
        active,
        sort_order,
        parameter_name
    )
    WHERE deleted = FALSE;

