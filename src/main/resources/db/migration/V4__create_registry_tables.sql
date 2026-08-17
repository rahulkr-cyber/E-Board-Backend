-- =====================================================================
-- V4: Registry tables (03_DATABASE.md section 7)
-- diary entries, diary metadata, receipt register
-- plus the shared number-sequence table used for concurrency-safe,
-- yearly-resetting number generation (diary/file/dispatch/receipt).
-- =====================================================================

CREATE TABLE core_number_sequences (
    id            UUID PRIMARY KEY,
    sequence_key  VARCHAR(100) NOT NULL,
    sequence_year INT NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_number_sequences UNIQUE (sequence_key, sequence_year)
);

CREATE TABLE registry_diary_entries (
    id                       UUID PRIMARY KEY,
    diary_number             VARCHAR(100) UNIQUE NOT NULL,
    diary_year               INT NOT NULL,
    diary_sequence           BIGINT NOT NULL,
    source_type              VARCHAR(50) NOT NULL,
    received_mode            VARCHAR(50) NOT NULL,
    received_date            TIMESTAMP NOT NULL,
    received_by              UUID NOT NULL,
    sender_name              VARCHAR(200),
    sender_designation       VARCHAR(200),
    sender_department        VARCHAR(200),
    sender_address           TEXT,
    sender_email             VARCHAR(200),
    sender_mobile            VARCHAR(20),
    original_letter_number   VARCHAR(150),
    reference_number         VARCHAR(150),
    letter_date              DATE,
    subject                  TEXT NOT NULL,
    description              TEXT,
    category_id              UUID,
    priority_id              UUID,
    language_id              UUID,
    confidential             BOOLEAN DEFAULT FALSE,
    due_date                 DATE,
    reminder_date            DATE,
    page_count               INT,
    physical_copy_received   BOOLEAN DEFAULT TRUE,
    barcode_value            VARCHAR(200),
    qr_code_value            TEXT,
    initial_department_id    UUID,
    initial_section_id       UUID,
    initial_assigned_user_id UUID,
    status                   VARCHAR(50) NOT NULL,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP,
    created_by               UUID,
    updated_by               UUID,
    deleted                  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_diary_number          ON registry_diary_entries(diary_number);
CREATE INDEX idx_diary_received_date   ON registry_diary_entries(received_date);
CREATE INDEX idx_diary_sender_name     ON registry_diary_entries(sender_name);
CREATE INDEX idx_diary_subject         ON registry_diary_entries(subject);
CREATE INDEX idx_diary_category        ON registry_diary_entries(category_id);
CREATE INDEX idx_diary_priority        ON registry_diary_entries(priority_id);
CREATE INDEX idx_diary_status          ON registry_diary_entries(status);
CREATE INDEX idx_diary_initial_section ON registry_diary_entries(initial_section_id);

CREATE TABLE registry_diary_metadata (
    id                     UUID PRIMARY KEY,
    diary_entry_id         UUID NOT NULL,
    metadata_definition_id UUID NOT NULL,
    field_value            TEXT,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP,
    created_by             UUID,
    updated_by             UUID,
    deleted                BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_diary_metadata_entry ON registry_diary_metadata(diary_entry_id);

CREATE TABLE registry_receipt_register (
    id             UUID PRIMARY KEY,
    diary_entry_id UUID NOT NULL,
    receipt_number VARCHAR(100) UNIQUE NOT NULL,
    received_from  VARCHAR(200),
    received_by    UUID NOT NULL,
    received_at    TIMESTAMP NOT NULL,
    remarks        TEXT,
    created_at     TIMESTAMP NOT NULL,
    created_by     UUID
);

CREATE INDEX idx_receipt_diary ON registry_receipt_register(diary_entry_id);
