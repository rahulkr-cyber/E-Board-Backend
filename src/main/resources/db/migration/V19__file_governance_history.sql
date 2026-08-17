-- =====================================================================
-- V19 (BCR-03 Parts 7-8): Priority-change and reopen history for files.
-- Both are append-only governance trails alongside the audit log.
-- =====================================================================

CREATE TABLE file_priority_change_history (
    id             UUID PRIMARY KEY,
    file_id        UUID NOT NULL,
    old_priority_id UUID,
    new_priority_id UUID NOT NULL,
    changed_by     UUID NOT NULL,
    changed_at     TIMESTAMP NOT NULL,
    remarks        VARCHAR(1000),
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,
    created_by     UUID,
    updated_by     UUID,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_prio_history_file ON file_priority_change_history(file_id);

CREATE TABLE file_reopen_history (
    id             UUID PRIMARY KEY,
    file_id        UUID NOT NULL,
    reopened_by    UUID NOT NULL,
    reopened_at    TIMESTAMP NOT NULL,
    reason         VARCHAR(1000) NOT NULL,
    previous_status VARCHAR(50) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,
    created_by     UUID,
    updated_by     UUID,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_reopen_history_file ON file_reopen_history(file_id);
