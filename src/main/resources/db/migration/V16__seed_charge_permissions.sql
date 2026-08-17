-- =====================================================================
-- V16: Transfer/posting permissions (CHARGE_MANAGE already seeded in V12).
-- =====================================================================

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000061', 'TRANSFER_MANAGE', 'ADMIN', 'Record transfers and joining/relieving', NOW(), FALSE),
('50000000-0000-0000-0000-000000000062', 'POSTING_VIEW',    'ADMIN', 'View posting/charge/transfer history', NOW(), FALSE);

-- SYSTEM_ADMIN gets both.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('TRANSFER_MANAGE', 'POSTING_VIEW') AND p.deleted = FALSE;

-- Leadership roles that manage charge also see posting history.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE r.code IN ('CHAIRPERSON', 'COMMISSIONER', 'OSD')
  AND p.code IN ('TRANSFER_MANAGE', 'POSTING_VIEW')
  AND r.deleted = FALSE AND p.deleted = FALSE;

-- Grant POSTING_VIEW to roles that already hold CHARGE_MANAGE, so charge
-- managers can review the history they act on.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, p.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions cm ON cm.id = rp.permission_id AND cm.code = 'CHARGE_MANAGE'
CROSS JOIN identity_permissions p
WHERE p.code = 'POSTING_VIEW' AND p.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1 FROM identity_role_permissions x
      WHERE x.role_id = rp.role_id AND x.permission_id = p.id);
