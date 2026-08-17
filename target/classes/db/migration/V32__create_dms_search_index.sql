-- Independent DMS search projection. This table does not reference or alter
-- Diary, Letter, Workflow, Dispatch or existing attachment search structures.

CREATE TABLE dms_search_index (
    id                      UUID PRIMARY KEY,
    document_id             UUID NOT NULL,
    document_number         VARCHAR(50) NOT NULL,
    document_type_id        UUID NOT NULL,
    document_type_code      VARCHAR(50) NOT NULL,
    document_type_name      VARCHAR(150) NOT NULL,
    title                   VARCHAR(250) NOT NULL,
    description             VARCHAR(2000),
    status                  VARCHAR(30) NOT NULL,
    current_version_number  INTEGER NOT NULL,
    uploaded_by             UUID NOT NULL,
    uploaded_by_name        VARCHAR(200),
    department_id           UUID,
    section_id              UUID,
    uploaded_at             TIMESTAMP NOT NULL,
    document_updated_at     TIMESTAMP,
    metadata_json           JSONB NOT NULL DEFAULT '{}'::jsonb,
    tags_json               JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata_text           TEXT,
    tags_text               TEXT,
    latest_file_name        VARCHAR(500),
    keywords_text           TEXT,
    ocr_text                TEXT,
    search_vector           TSVECTOR GENERATED ALWAYS AS (
        SETWEIGHT(TO_TSVECTOR('simple'::regconfig, COALESCE(document_number, '')), 'A') ||
        SETWEIGHT(TO_TSVECTOR('simple'::regconfig, COALESCE(title, '')), 'A') ||
        SETWEIGHT(TO_TSVECTOR('simple'::regconfig,
            COALESCE(document_type_code, '') || ' ' || COALESCE(document_type_name, '')), 'B') ||
        SETWEIGHT(TO_TSVECTOR('simple'::regconfig,
            COALESCE(tags_text, '') || ' ' || COALESCE(metadata_text, '')), 'B') ||
        SETWEIGHT(TO_TSVECTOR('simple'::regconfig,
            COALESCE(description, '') || ' ' || COALESCE(latest_file_name, '') || ' '
            || COALESCE(keywords_text, '') || ' ' || COALESCE(ocr_text, '')), 'C')
    ) STORED,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP,
    created_by              UUID,
    updated_by              UUID,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dms_search_document
        FOREIGN KEY (document_id) REFERENCES dms_documents (id),
    CONSTRAINT fk_dms_search_document_type
        FOREIGN KEY (document_type_id) REFERENCES dms_document_types (id),
    CONSTRAINT fk_dms_search_uploader
        FOREIGN KEY (uploaded_by) REFERENCES identity_users (id),
    CONSTRAINT ck_dms_search_current_version
        CHECK (current_version_number >= 1)
);

CREATE UNIQUE INDEX uq_dms_search_document
    ON dms_search_index (document_id)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_search_vector
    ON dms_search_index USING GIN (search_vector)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_search_metadata_json
    ON dms_search_index USING GIN (metadata_json)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_search_tags_json
    ON dms_search_index USING GIN (tags_json)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_search_type_status_date
    ON dms_search_index (document_type_id, status, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_search_scope
    ON dms_search_index (uploaded_by, department_id, section_id, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX idx_dms_search_document_number
    ON dms_search_index (LOWER(document_number))
    WHERE deleted = FALSE;

-- Backfill documents created before the search engine is deployed.
WITH metadata_aggregate AS (
    SELECT m.document_id,
           JSONB_OBJECT_AGG(m.field_key, m.value_json ORDER BY m.field_key) AS metadata_json,
           STRING_AGG(m.field_label || ' ' || m.value_json::text, ' ' ORDER BY m.field_key) AS metadata_text
    FROM dms_document_metadata m
    WHERE m.deleted = FALSE
      AND m.searchable = TRUE
    GROUP BY m.document_id
),
tag_aggregate AS (
    SELECT t.document_id,
           JSONB_AGG(t.tag_value ORDER BY t.tag_value) AS tags_json,
           STRING_AGG(t.tag_value, ' ' ORDER BY t.tag_value) AS tags_text
    FROM dms_document_tags t
    WHERE t.deleted = FALSE
    GROUP BY t.document_id
),
latest_version AS (
    SELECT DISTINCT ON (v.document_id)
           v.document_id,
           v.original_file_name,
           v.version_comment
    FROM dms_document_versions v
    WHERE v.deleted = FALSE
    ORDER BY v.document_id, v.version_number DESC
)
INSERT INTO dms_search_index (
    id,
    document_id,
    document_number,
    document_type_id,
    document_type_code,
    document_type_name,
    title,
    description,
    status,
    current_version_number,
    uploaded_by,
    uploaded_by_name,
    department_id,
    section_id,
    uploaded_at,
    document_updated_at,
    metadata_json,
    tags_json,
    metadata_text,
    tags_text,
    latest_file_name,
    keywords_text,
    ocr_text,
    created_at,
    updated_at,
    created_by,
    updated_by,
    deleted
)
SELECT d.id,
       d.id,
       d.document_number,
       d.document_type_id,
       dt.code,
       dt.name,
       d.title,
       d.description,
       d.status,
       d.current_version_number,
       d.uploaded_by,
       u.full_name,
       d.department_id,
       d.section_id,
       d.uploaded_at,
       d.updated_at,
       COALESCE(ma.metadata_json, '{}'::jsonb),
       COALESCE(ta.tags_json, '[]'::jsonb),
       ma.metadata_text,
       ta.tags_text,
       lv.original_file_name,
       CONCAT_WS(' ', d.document_number, d.title, d.description, d.status,
                 dt.code, dt.name, u.full_name, ta.tags_text, ma.metadata_text,
                 lv.original_file_name, lv.version_comment),
       NULL,
       CURRENT_TIMESTAMP,
       NULL,
       NULL,
       NULL,
       FALSE
FROM dms_documents d
JOIN dms_document_types dt
  ON dt.id = d.document_type_id
LEFT JOIN identity_users u
  ON u.id = d.uploaded_by
LEFT JOIN metadata_aggregate ma
  ON ma.document_id = d.id
LEFT JOIN tag_aggregate ta
  ON ta.document_id = d.id
LEFT JOIN latest_version lv
  ON lv.document_id = d.id
WHERE d.deleted = FALSE;
