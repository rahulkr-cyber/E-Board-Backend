-- =====================================================================
-- V26: DMS module permissions.
--
-- Additive RBAC integration only. No existing table or permission is altered.
-- SYSTEM_ADMIN receives the initial DMS permissions; other role assignments
-- remain configurable through the existing role-management module.
-- =====================================================================

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000091', 'DMS_VIEW',               'DMS', 'View DMS documents and dashboard',       NOW(), FALSE),
('50000000-0000-0000-0000-000000000092', 'DMS_CREATE',             'DMS', 'Create DMS document records',            NOW(), FALSE),
('50000000-0000-0000-0000-000000000093', 'DMS_UPDATE',             'DMS', 'Update DMS document metadata',           NOW(), FALSE),
('50000000-0000-0000-0000-000000000094', 'DMS_DELETE',             'DMS', 'Delete DMS documents',                   NOW(), FALSE),
('50000000-0000-0000-0000-000000000095', 'DMS_ADMIN',              'DMS', 'Administer the DMS module',              NOW(), FALSE),
('50000000-0000-0000-0000-000000000096', 'DMS_DOWNLOAD',           'DMS', 'Download DMS document versions',         NOW(), FALSE),
('50000000-0000-0000-0000-000000000097', 'DMS_UPLOAD',             'DMS', 'Upload DMS documents and versions',      NOW(), FALSE),
('50000000-0000-0000-0000-000000000098', 'DMS_SHARE',              'DMS', 'Share DMS documents',                    NOW(), FALSE),
('50000000-0000-0000-0000-000000000099', 'DMS_METADATA_ADMIN',     'DMS', 'Configure DMS metadata fields',          NOW(), FALSE),
('50000000-0000-0000-0000-000000000100', 'DMS_DOCUMENTTYPE_ADMIN', 'DMS', 'Configure DMS document types',           NOW(), FALSE),
('50000000-0000-0000-0000-000000000101', 'DMS_MASTERDATA_ADMIN',   'DMS', 'Configure DMS master data sources',      NOW(), FALSE),
('50000000-0000-0000-0000-000000000102', 'DMS_AUDIT_VIEW',         'DMS', 'View DMS audit history',                 NOW(), FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(),
       '40000000-0000-0000-0000-000000000001',
       p.id,
       NOW()
FROM identity_permissions p
WHERE p.module = 'DMS'
  AND p.code IN ('DMS_VIEW', 'DMS_CREATE', 'DMS_UPDATE', 'DMS_DELETE',
                 'DMS_ADMIN', 'DMS_DOWNLOAD', 'DMS_UPLOAD', 'DMS_SHARE',
                 'DMS_METADATA_ADMIN', 'DMS_DOCUMENTTYPE_ADMIN',
                 'DMS_MASTERDATA_ADMIN', 'DMS_AUDIT_VIEW')
  AND p.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;
