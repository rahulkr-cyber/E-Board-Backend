-- =====================================================================
-- V7: Additional workflow permissions (Phase 5)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------------
INSERT INTO identity_permissions
(id, code, module, description, created_at, deleted)
VALUES
('50000000-0000-0000-0000-000000000041', 'WORKFLOW_START',   'WORKFLOW', 'Start a workflow on a file', NOW(), FALSE),
('50000000-0000-0000-0000-000000000042', 'WORKFLOW_FORWARD', 'WORKFLOW', 'Forward a file in a workflow', NOW(), FALSE),
('50000000-0000-0000-0000-000000000043', 'WORKFLOW_VIEW',    'WORKFLOW', 'View workflow tasks and history', NOW(), FALSE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------
-- SYSTEM_ADMIN
-- ---------------------------------------------------------------------
INSERT INTO identity_role_permissions
(id, role_id, permission_id, created_at)
SELECT
    gen_random_uuid(),
    '40000000-0000-0000-0000-000000000001',
    p.id,
    NOW()
FROM identity_permissions p
WHERE p.code IN (
    'WORKFLOW_START',
    'WORKFLOW_FORWARD',
    'WORKFLOW_VIEW'
)
AND p.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- CHAIRPERSON, COMMISSIONER, OSD, SECTION_OFFICER,
-- ASSISTANT_SECTION_OFFICER
-- ---------------------------------------------------------------------
INSERT INTO identity_role_permissions
(id, role_id, permission_id, created_at)
SELECT
    gen_random_uuid(),
    r.id,
    p.id,
    NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE r.code IN (
    'CHAIRPERSON',
    'COMMISSIONER',
    'OSD',
    'SECTION_OFFICER',
    'ASSISTANT_SECTION_OFFICER'
)
AND p.code IN (
    'WORKFLOW_START',
    'WORKFLOW_FORWARD',
    'WORKFLOW_VIEW'
)
AND r.deleted = FALSE
AND p.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- READ_ONLY, RECORD_ROOM
-- ---------------------------------------------------------------------
INSERT INTO identity_role_permissions
(id, role_id, permission_id, created_at)
SELECT
    gen_random_uuid(),
    r.id,
    p.id,
    NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE r.code IN (
    'READ_ONLY',
    'RECORD_ROOM'
)
AND p.code = 'WORKFLOW_VIEW'
AND r.deleted = FALSE
AND p.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;