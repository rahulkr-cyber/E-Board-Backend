-- =====================================================================
-- V9: Seed notification permissions, default escalation ladder, and
-- notification templates.
-- =====================================================================

-- Notification permission (viewing/among own feed). Everyone authenticated
-- can read their own feed; we still gate the endpoints on this permission
-- and grant it broadly.
INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000051', 'NOTIFICATION_VIEW', 'NOTIFICATION', 'View own notifications', NOW(), FALSE),
('50000000-0000-0000-0000-000000000052', 'ESCALATION_CONFIGURE', 'NOTIFICATION', 'Configure escalation rules', NOW(), FALSE);

-- Grant NOTIFICATION_VIEW to every seeded role (it is inherently self-scoped).
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE p.code = 'NOTIFICATION_VIEW'
  AND r.deleted = FALSE AND p.deleted = FALSE;

-- ESCALATION_CONFIGURE only to SYSTEM_ADMIN.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p
WHERE p.code = 'ESCALATION_CONFIGURE' AND p.deleted = FALSE;

-- Default escalation ladder (06_BUSINESS_RULES.md section 10):
-- 3d reminder, 7d warning, 15d escalation, 30d top-level.
INSERT INTO workflow_escalation_rules
    (id, code, name, days_after_due, escalation_level, notify_role_id, active, created_at, deleted) VALUES
('c0000000-0000-0000-0000-000000000001', 'REMINDER_3D',   'Reminder (3 days overdue)',        3,  'REMINDER',         NULL, TRUE, NOW(), FALSE),
('c0000000-0000-0000-0000-000000000002', 'WARNING_7D',    'Warning (7 days overdue)',         7,  'WARNING',          NULL, TRUE, NOW(), FALSE),
('c0000000-0000-0000-0000-000000000003', 'ESCALATION_15D','Escalation (15 days overdue)',     15, 'ESCALATION',       '40000000-0000-0000-0000-000000000004', TRUE, NOW(), FALSE),
('c0000000-0000-0000-0000-000000000004', 'TOP_30D',       'Top-level escalation (30 days)',   30, 'TOP_ESCALATION',   '40000000-0000-0000-0000-000000000003', TRUE, NOW(), FALSE);

-- Notification templates (placeholders resolved by the service).
INSERT INTO notification_templates
    (id, code, title_template, message_template, notification_type, active, created_at, deleted) VALUES
('d0000000-0000-0000-0000-000000000001', 'ASSIGNMENT',       'New file assigned: {fileNumber}',   'File {fileNumber} ({subject}) has been assigned to you.', 'ASSIGNMENT', TRUE, NOW(), FALSE),
('d0000000-0000-0000-0000-000000000002', 'FORWARD',          'File forwarded: {fileNumber}',      'File {fileNumber} has been forwarded to you.',            'FORWARD', TRUE, NOW(), FALSE),
('d0000000-0000-0000-0000-000000000003', 'RETURN',           'File returned: {fileNumber}',       'File {fileNumber} was returned. Remarks: {remarks}',      'RETURN', TRUE, NOW(), FALSE),
('d0000000-0000-0000-0000-000000000004', 'APPROVAL_PENDING', 'Approval pending: {fileNumber}',    'File {fileNumber} awaits your approval.',                 'APPROVAL_PENDING', TRUE, NOW(), FALSE),
('d0000000-0000-0000-0000-000000000005', 'REJECTION',        'File rejected: {fileNumber}',       'File {fileNumber} was rejected. Remarks: {remarks}',      'REJECTION', TRUE, NOW(), FALSE),
('d0000000-0000-0000-0000-000000000006', 'ESCALATION',       'Escalation: {fileNumber}',          'File {fileNumber} is overdue ({level}). Please act.',     'ESCALATION', TRUE, NOW(), FALSE);
