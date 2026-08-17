-- =====================================================================
-- V14: Dispatch permissions (Phase 4 - Correspondence)
-- FILE_*, LETTER_* already seeded in V12; add DISPATCH_* here and grant.
-- =====================================================================

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000031', 'DISPATCH_CREATE', 'CORRESPONDENCE', 'Create dispatch record', NOW(), FALSE),
('50000000-0000-0000-0000-000000000032', 'DISPATCH_VIEW',   'CORRESPONDENCE', 'View dispatch register', NOW(), FALSE),
('50000000-0000-0000-0000-000000000033', 'DISPATCH_UPDATE', 'CORRESPONDENCE', 'Update dispatch status', NOW(), FALSE) ON CONFLICT (code) DO NOTHING;

-- Grant dispatch permissions to SYSTEM_ADMIN
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(),
       '40000000-0000-0000-0000-000000000001',
       p.id,
       NOW()
FROM identity_permissions p
WHERE p.code IN ('DISPATCH_CREATE', 'DISPATCH_VIEW', 'DISPATCH_UPDATE')
  AND p.deleted = FALSE ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Grant to DISPATCH_CLERK (create/view/update dispatch, plus view letters/files)
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(),
       '40000000-0000-0000-0000-000000000009',
       p.id,
       NOW()
FROM identity_permissions p
WHERE p.code IN ('DISPATCH_CREATE', 'DISPATCH_VIEW', 'DISPATCH_UPDATE',
                 'FILE_VIEW', 'LETTER_VIEW', 'LETTER_CREATE')
  AND p.deleted = FALSE ON CONFLICT (role_id, permission_id) DO NOTHING;
