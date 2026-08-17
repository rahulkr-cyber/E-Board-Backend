-- Central viewer action permissions, using the existing RBAC tables.
INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000131', 'DOCUMENT_DOWNLOAD', 'DOCUMENT_VIEWER', 'Download documents through the secure viewer', NOW(), FALSE),
('50000000-0000-0000-0000-000000000132', 'DOCUMENT_PRINT',    'DOCUMENT_VIEWER', 'Print documents through the secure viewer', NOW(), FALSE),
('50000000-0000-0000-0000-000000000133', 'DOCUMENT_SHARE',    'DOCUMENT_VIEWER', 'Create temporary secure document links', NOW(), FALSE)
ON CONFLICT (code) DO NOTHING;

-- Preserve existing download/print behavior for roles that already view records
-- or already export/download documents.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, target.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions existing ON existing.id = rp.permission_id
CROSS JOIN identity_permissions target
WHERE existing.code IN (
    'DIARY_VIEW', 'LETTER_VIEW', 'FILE_VIEW', 'DISPATCH_VIEW', 'POSTING_VIEW',
    'TRANSFER_MANAGE', 'CHARGE_MANAGE', 'DASHBOARD_VIEW', 'USER_VIEW',
    'CHECKLIST_VIEW', 'CHECKLIST_UPLOAD', 'CHECKLIST_VERIFY', 'CHECKLIST_ADMIN',
    'DMS_DOWNLOAD', 'DMS_ADMIN', 'REPORT_EXPORT')
  AND target.code IN ('DOCUMENT_DOWNLOAD', 'DOCUMENT_PRINT')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Sharing stays restricted to roles already trusted to share DMS content.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, target.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions existing ON existing.id = rp.permission_id
CROSS JOIN identity_permissions target
WHERE existing.code IN ('DMS_SHARE', 'DMS_ADMIN')
  AND target.code = 'DOCUMENT_SHARE'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- System administrator receives every viewer action.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DOCUMENT_DOWNLOAD', 'DOCUMENT_PRINT', 'DOCUMENT_SHARE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
