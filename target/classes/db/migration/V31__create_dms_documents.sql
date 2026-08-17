-- DMS document persistence and immutable file version history.
-- Existing attachment tables and storage are intentionally not referenced.

CREATE SEQUENCE dms_document_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE dms_documents (
    id                      UUID PRIMARY KEY,
    document_type_id        UUID NOT NULL,
    document_number         VARCHAR(50) NOT NULL,
    title                   VARCHAR(250) NOT NULL,
    description             VARCHAR(2000),
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    current_version_number  INTEGER NOT NULL DEFAULT 1,
    uploaded_by             UUID NOT NULL,
    department_id           UUID,
    section_id              UUID,
    uploaded_at             TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP,
    created_by              UUID,
    updated_by              UUID,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_document_type
        FOREIGN KEY (document_type_id) REFERENCES dms_document_types (id),
    CONSTRAINT fk_dms_document_uploader
        FOREIGN KEY (uploaded_by) REFERENCES identity_users (id),
    CONSTRAINT ck_dms_document_current_version
        CHECK (current_version_number >= 1)
);

CREATE UNIQUE INDEX uq_dms_documents_number
    ON dms_documents (LOWER(document_number))
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_documents_type_status
    ON dms_documents (document_type_id, status, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_documents_uploader
    ON dms_documents (uploaded_by, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_documents_department_section
    ON dms_documents (department_id, section_id, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE TABLE dms_document_versions (
    id                  UUID PRIMARY KEY,
    document_id         UUID NOT NULL,
    version_number      INTEGER NOT NULL,
    storage_provider    VARCHAR(60) NOT NULL,
    storage_key         VARCHAR(1000) NOT NULL,
    original_file_name  VARCHAR(500) NOT NULL,
    mime_type           VARCHAR(255) NOT NULL,
    file_size           BIGINT NOT NULL,
    checksum_sha256     VARCHAR(64) NOT NULL,
    version_comment     VARCHAR(1000),
    uploaded_by         UUID NOT NULL,
    uploaded_at         TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_version_document
        FOREIGN KEY (document_id) REFERENCES dms_documents (id),
    CONSTRAINT fk_dms_version_uploader
        FOREIGN KEY (uploaded_by) REFERENCES identity_users (id),
    CONSTRAINT ck_dms_version_number
        CHECK (version_number >= 1),
    CONSTRAINT ck_dms_version_file_size
        CHECK (file_size > 0)
);

CREATE UNIQUE INDEX uq_dms_document_version_number
    ON dms_document_versions (document_id, version_number)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uq_dms_document_version_storage
    ON dms_document_versions (storage_provider, storage_key)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_document_versions_document
    ON dms_document_versions (document_id, version_number DESC)
    WHERE deleted = FALSE;

CREATE TABLE dms_document_metadata (
    id                  UUID PRIMARY KEY,
    document_id         UUID NOT NULL,
    metadata_field_id   UUID NOT NULL,
    field_key           VARCHAR(80) NOT NULL,
    field_label         VARCHAR(150) NOT NULL,
    value_json          JSONB NOT NULL,
    searchable          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_document_metadata_document
        FOREIGN KEY (document_id) REFERENCES dms_documents (id),
    CONSTRAINT fk_dms_document_metadata_field
        FOREIGN KEY (metadata_field_id) REFERENCES dms_metadata_fields (id)
);

CREATE UNIQUE INDEX uq_dms_document_metadata_field
    ON dms_document_metadata (document_id, metadata_field_id)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_document_metadata_key
    ON dms_document_metadata (field_key)
    WHERE deleted = FALSE AND searchable = TRUE;

CREATE INDEX idx_dms_document_metadata_value_gin
    ON dms_document_metadata USING GIN (value_json)
    WHERE deleted = FALSE AND searchable = TRUE;

CREATE TABLE dms_document_tags (
    id          UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    tag_value   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_document_tag_document
        FOREIGN KEY (document_id) REFERENCES dms_documents (id)
);

CREATE UNIQUE INDEX uq_dms_document_tag
    ON dms_document_tags (document_id, LOWER(tag_value))
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_document_tags_value
    ON dms_document_tags (LOWER(tag_value))
    WHERE deleted = FALSE;
