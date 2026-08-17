-- Independent configurable metadata engine for the DMS module.
-- No existing attachment or business-module tables are changed.

CREATE TABLE dms_metadata_fields (
    id                              UUID PRIMARY KEY,
    document_type_id                UUID NOT NULL,
    field_key                       VARCHAR(80) NOT NULL,
    label                           VARCHAR(150) NOT NULL,
    control_type                    VARCHAR(30) NOT NULL,
    placeholder                     VARCHAR(250),
    help_text                       VARCHAR(1000),
    default_value                   VARCHAR(4000),
    sort_order                      INTEGER NOT NULL DEFAULT 0,
    required                        BOOLEAN NOT NULL DEFAULT FALSE,
    read_only                       BOOLEAN NOT NULL DEFAULT FALSE,
    hidden                          BOOLEAN NOT NULL DEFAULT FALSE,
    searchable                      BOOLEAN NOT NULL DEFAULT TRUE,
    active                          BOOLEAN NOT NULL DEFAULT TRUE,
    min_length                      INTEGER,
    max_length                      INTEGER,
    min_value                       NUMERIC(38, 10),
    max_value                       NUMERIC(38, 10),
    regex_pattern                   VARCHAR(1000),
    date_constraint                 VARCHAR(30) NOT NULL DEFAULT 'NONE',
    visibility_condition_field_key  VARCHAR(80),
    visibility_condition_operator   VARCHAR(20),
    visibility_condition_value      VARCHAR(2000),
    mandatory_condition_field_key   VARCHAR(80),
    mandatory_condition_operator    VARCHAR(20),
    mandatory_condition_value       VARCHAR(2000),
    master_source_id                UUID,
    parent_field_id                 UUID,
    source_parameter_name           VARCHAR(80),
    created_at                      TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP,
    created_by                      UUID,
    updated_by                      UUID,
    deleted                         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_metadata_document_type
        FOREIGN KEY (document_type_id) REFERENCES dms_document_types (id),
    CONSTRAINT fk_dms_metadata_parent_field
        FOREIGN KEY (parent_field_id) REFERENCES dms_metadata_fields (id),
    CONSTRAINT ck_dms_metadata_sort_order
        CHECK (sort_order >= 0),
    CONSTRAINT ck_dms_metadata_length_range
        CHECK (min_length IS NULL OR max_length IS NULL OR min_length <= max_length),
    CONSTRAINT ck_dms_metadata_numeric_range
        CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
);

CREATE UNIQUE INDEX uq_dms_metadata_field_key
    ON dms_metadata_fields (document_type_id, LOWER(field_key))
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_metadata_document_type_order
    ON dms_metadata_fields (document_type_id, active, sort_order, label)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_metadata_parent_field
    ON dms_metadata_fields (parent_field_id)
    WHERE deleted = FALSE AND parent_field_id IS NOT NULL;

CREATE INDEX idx_dms_metadata_master_source
    ON dms_metadata_fields (master_source_id)
    WHERE deleted = FALSE AND master_source_id IS NOT NULL;

CREATE TABLE dms_metadata_options (
    id                  UUID PRIMARY KEY,
    metadata_field_id   UUID NOT NULL,
    option_value        VARCHAR(500) NOT NULL,
    option_label        VARCHAR(500) NOT NULL,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_metadata_option_field
        FOREIGN KEY (metadata_field_id) REFERENCES dms_metadata_fields (id),
    CONSTRAINT ck_dms_metadata_option_sort_order
        CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uq_dms_metadata_option_value
    ON dms_metadata_options (metadata_field_id, LOWER(option_value))
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_metadata_option_order
    ON dms_metadata_options (metadata_field_id, active, sort_order, option_label)
    WHERE deleted = FALSE;
