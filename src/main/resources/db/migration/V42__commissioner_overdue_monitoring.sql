-- =====================================================================
-- V41: Commissioner overdue monitoring support.
-- Additive only. Ownership and workflow assignment columns are untouched.
-- =====================================================================

-- A breach is recorded/notified on the first daily sweep after the due date.
-- The existing unique (task_id, escalation_level) constraint prevents repeats.
INSERT INTO workflow_escalation_rules
    (id, code, name, days_after_due, escalation_level, notify_role_id,
     active, created_at, deleted)
VALUES
    ('c0000000-0000-0000-0000-000000000005', 'OVERDUE_1D',
     'Initial SLA breach visibility', 1, 'OVERDUE',
     (SELECT id FROM identity_roles
      WHERE code = 'COMMISSIONER' AND deleted = FALSE
      ORDER BY created_at LIMIT 1), TRUE, NOW(), FALSE)
ON CONFLICT (code) DO NOTHING;

-- Keep the existing template code/API but make it work for files and letters.
UPDATE notification_templates
SET title_template = 'Overdue workflow item: {reference}',
    message_template = '{reference} ({subject}) is {days} day(s) overdue at {level} level.',
    updated_at = NOW()
WHERE code = 'ESCALATION' AND deleted = FALSE;

-- Dashboard/scheduler hot paths.
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_pending_due
    ON workflow_tasks(due_date, assigned_to_section_id, assigned_to_user_id)
    WHERE status = 'PENDING' AND due_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_task_escalations_task_days
    ON workflow_task_escalations(task_id, days_after_due DESC);

-- Escalation history is an immutable SLA audit record, like workflow movements.
CREATE OR REPLACE FUNCTION trg_block_task_escalation_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'workflow_task_escalations is immutable: % not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS block_task_escalation_update ON workflow_task_escalations;
CREATE TRIGGER block_task_escalation_update
    BEFORE UPDATE ON workflow_task_escalations
    FOR EACH ROW EXECUTE FUNCTION trg_block_task_escalation_mutation();

DROP TRIGGER IF EXISTS block_task_escalation_delete ON workflow_task_escalations;
CREATE TRIGGER block_task_escalation_delete
    BEFORE DELETE ON workflow_task_escalations
    FOR EACH ROW EXECUTE FUNCTION trg_block_task_escalation_mutation();
