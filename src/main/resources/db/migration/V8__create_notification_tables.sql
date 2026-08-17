-- =====================================================================
-- V8: Notification & escalation tables (03_DATABASE.md sections 10.7, 11)
-- In-app notifications, reusable templates, and configurable escalation
-- rules consumed by the daily scheduler (Phase 6).
-- =====================================================================

CREATE TABLE notification_notifications (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL,
    title              VARCHAR(255) NOT NULL,
    message            TEXT NOT NULL,
    notification_type  VARCHAR(50) NOT NULL,
    priority           VARCHAR(50),
    linked_entity_type VARCHAR(50),
    linked_entity_id   UUID,
    read_flag          BOOLEAN NOT NULL DEFAULT FALSE,
    read_at            TIMESTAMP,
    expires_at         TIMESTAMP,
    created_at         TIMESTAMP NOT NULL,
    created_by         UUID
);

CREATE INDEX idx_notifications_user ON notification_notifications(user_id);
CREATE INDEX idx_notifications_user_unread
    ON notification_notifications(user_id, read_flag);
CREATE INDEX idx_notifications_created ON notification_notifications(created_at);

CREATE TABLE notification_templates (
    id                UUID PRIMARY KEY,
    code              VARCHAR(100) UNIQUE NOT NULL,
    title_template    TEXT NOT NULL,
    message_template  TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP,
    created_by        UUID,
    updated_by        UUID,
    deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

-- Configurable escalation ladder (03_DATABASE.md 10.7). The daily
-- scheduler matches overdue tasks to the highest rule whose
-- days_after_due threshold has been crossed.
CREATE TABLE workflow_escalation_rules (
    id                    UUID PRIMARY KEY,
    code                  VARCHAR(100) UNIQUE NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    days_after_due        INT NOT NULL,
    escalation_level      VARCHAR(50) NOT NULL,
    notify_role_id        UUID,
    notify_designation_id UUID,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    created_by            UUID,
    updated_by            UUID,
    deleted               BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_escalation_rules_days ON workflow_escalation_rules(days_after_due);

-- Tracks which escalation level has already fired for a task, so the
-- scheduler never raises a duplicate reminder for the same level
-- (06_BUSINESS_RULES.md section 10 rule 2).
CREATE TABLE workflow_task_escalations (
    id               UUID PRIMARY KEY,
    task_id          UUID NOT NULL,
    file_id          UUID NOT NULL,
    escalation_level VARCHAR(50) NOT NULL,
    days_after_due   INT NOT NULL,
    escalated_at     TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    CONSTRAINT uq_task_escalation_level UNIQUE (task_id, escalation_level)
);

CREATE INDEX idx_task_escalations_task ON workflow_task_escalations(task_id);
