-- =====================================================================
-- V5: Correspondence tables (03_DATABASE.md section 8)
-- files, letters, attachments, followups, reminders, dispatch register.
-- Schema created in full; services arrive with Phases 3 (attachments,
-- inward letters via facade) and 4 (files, letters, dispatch).
-- =====================================================================

CREATE TABLE correspondence_files (
    id                 UUID PRIMARY KEY,
    file_number        VARCHAR(100) UNIQUE NOT NULL,
    file_year          INT NOT NULL,
    file_sequence      BIGINT NOT NULL,
    subject            TEXT NOT NULL,
    description        TEXT,
    category_id        UUID,
    priority_id        UUID,
    department_id      UUID,
    section_id         UUID,
    current_owner_id   UUID,
    current_section_id UUID,
    current_status     VARCHAR(50) NOT NULL,
    opened_date        DATE NOT NULL,
    closed_date        DATE,
    archived_date      DATE,
    confidential       BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP,
    created_by         UUID,
    updated_by         UUID,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_files_number          ON correspondence_files(file_number);
CREATE INDEX idx_files_status          ON correspondence_files(current_status);
CREATE INDEX idx_files_current_section ON correspondence_files(current_section_id);
CREATE INDEX idx_files_current_owner   ON correspondence_files(current_owner_id);

CREATE TABLE correspondence_letters (
    id                     UUID PRIMARY KEY,
    file_id                UUID,
    diary_entry_id         UUID,
    letter_direction       VARCHAR(20) NOT NULL,
    letter_type            VARCHAR(50) NOT NULL,
    letter_number          VARCHAR(150),
    reference_number       VARCHAR(150),
    letter_date            DATE,
    subject                TEXT NOT NULL,
    body                   TEXT,
    sender_name            VARCHAR(200),
    sender_designation     VARCHAR(200),
    sender_department      VARCHAR(200),
    sender_address         TEXT,
    receiver_department_id UUID,
    receiver_section_id    UUID,
    receiver_user_id       UUID,
    category_id            UUID,
    priority_id            UUID,
    language_id            UUID,
    confidential           BOOLEAN DEFAULT FALSE,
    due_date               DATE,
    reminder_date          DATE,
    current_owner_id       UUID,
    current_section_id     UUID,
    current_status         VARCHAR(50) NOT NULL,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP,
    created_by             UUID,
    updated_by             UUID,
    deleted                BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_letters_file           ON correspondence_letters(file_id);
CREATE INDEX idx_letters_diary          ON correspondence_letters(diary_entry_id);
CREATE INDEX idx_letters_status         ON correspondence_letters(current_status);
CREATE INDEX idx_letters_number         ON correspondence_letters(letter_number);
CREATE INDEX idx_letters_current_section ON correspondence_letters(current_section_id);

CREATE TABLE correspondence_attachments (
    id                 UUID PRIMARY KEY,
    linked_entity_type VARCHAR(50) NOT NULL,
    linked_entity_id   UUID NOT NULL,
    document_type_id   UUID,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name   VARCHAR(255) NOT NULL,
    file_extension     VARCHAR(20),
    mime_type          VARCHAR(100),
    file_size          BIGINT,
    storage_path       TEXT NOT NULL,
    checksum           VARCHAR(256),
    uploaded_by        UUID NOT NULL,
    uploaded_at        TIMESTAMP NOT NULL,
    active             BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP,
    created_by         UUID,
    updated_by         UUID,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_attachments_entity ON correspondence_attachments(linked_entity_type, linked_entity_id);

CREATE TABLE correspondence_followups (
    id                  UUID PRIMARY KEY,
    file_id             UUID NOT NULL,
    letter_id           UUID,
    followup_date       DATE NOT NULL,
    followup_type       VARCHAR(50),
    remarks             TEXT,
    next_followup_date  DATE,
    reply_received      BOOLEAN DEFAULT FALSE,
    reply_received_date DATE,
    status              VARCHAR(50) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_followups_file ON correspondence_followups(file_id);

CREATE TABLE correspondence_reminders (
    id              UUID PRIMARY KEY,
    file_id         UUID,
    letter_id       UUID,
    reminder_number VARCHAR(100),
    reminder_date   DATE NOT NULL,
    reminder_type   VARCHAR(50),
    remarks         TEXT,
    generated_by    UUID,
    status          VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      UUID,
    updated_by      UUID,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_reminders_file ON correspondence_reminders(file_id);

CREATE TABLE correspondence_dispatch_register (
    id                   UUID PRIMARY KEY,
    letter_id            UUID NOT NULL,
    dispatch_number      VARCHAR(100) UNIQUE NOT NULL,
    dispatch_date        DATE NOT NULL,
    dispatch_mode        VARCHAR(50),
    recipient_name       VARCHAR(200),
    recipient_department VARCHAR(200),
    recipient_address    TEXT,
    tracking_number      VARCHAR(150),
    status               VARCHAR(50) NOT NULL,
    remarks              TEXT,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP,
    created_by           UUID,
    updated_by           UUID,
    deleted              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_dispatch_letter ON correspondence_dispatch_register(letter_id);
