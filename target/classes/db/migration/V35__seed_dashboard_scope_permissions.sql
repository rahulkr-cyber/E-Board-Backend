-- =====================================================================
-- V35: Permission-scoped reusable dashboard views.
--
-- Additive only: the legacy DASHBOARD_VIEW permission remains the module
-- entry permission. These permissions govern which data scopes a user may
-- select in the shared dashboard. Roles remain configurable through the
-- existing role administration screens.
-- =====================================================================

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000111', 'DASHBOARD_VIEW_SELF',         'DASHBOARD', 'View own dashboard scope',              NOW(), FALSE),
('50000000-0000-0000-0000-000000000112', 'DASHBOARD_VIEW_SECTION',      'DASHBOARD', 'View a section dashboard scope',         NOW(), FALSE),
('50000000-0000-0000-0000-000000000113', 'DASHBOARD_VIEW_DEPARTMENT',   'DASHBOARD', 'View a department dashboard scope',      NOW(), FALSE),
('50000000-0000-0000-0000-000000000114', 'DASHBOARD_VIEW_OFFICER',      'DASHBOARD', 'View an officer dashboard scope',         NOW(), FALSE),
('50000000-0000-0000-0000-000000000115', 'DASHBOARD_VIEW_USER',         'DASHBOARD', 'View another user dashboard scope',      NOW(), FALSE),
('50000000-0000-0000-0000-000000000116', 'DASHBOARD_VIEW_ORGANIZATION', 'DASHBOARD', 'View the entire organization dashboard', NOW(), FALSE)
ON CONFLICT (code) DO NOTHING;

-- Every role that already has dashboard access receives SELF.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, scoped.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions legacy ON legacy.id = rp.permission_id
JOIN identity_permissions scoped ON scoped.code = 'DASHBOARD_VIEW_SELF'
WHERE legacy.code = 'DASHBOARD_VIEW'
  AND legacy.deleted = FALSE
  AND scoped.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- System Administrator and Chairman may select every scope.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), role.id, permission.id, NOW()
FROM identity_roles role
CROSS JOIN identity_permissions permission
WHERE role.code IN ('SYSTEM_ADMIN', 'CHAIRPERSON')
  AND permission.code IN (
      'DASHBOARD_VIEW_SELF', 'DASHBOARD_VIEW_SECTION',
      'DASHBOARD_VIEW_DEPARTMENT', 'DASHBOARD_VIEW_OFFICER',
      'DASHBOARD_VIEW_USER', 'DASHBOARD_VIEW_ORGANIZATION')
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Commissioner keeps the prior organization-wide dashboard and gains
-- human-readable drill-down scopes.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), role.id, permission.id, NOW()
FROM identity_roles role
CROSS JOIN identity_permissions permission
WHERE role.code = 'COMMISSIONER'
  AND permission.code IN (
      'DASHBOARD_VIEW_SELF', 'DASHBOARD_VIEW_SECTION',
      'DASHBOARD_VIEW_DEPARTMENT', 'DASHBOARD_VIEW_OFFICER',
      'DASHBOARD_VIEW_USER', 'DASHBOARD_VIEW_ORGANIZATION')
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- OSD previously used the global dashboard; preserve that visibility while
-- allowing operational drill-down.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), role.id, permission.id, NOW()
FROM identity_roles role
CROSS JOIN identity_permissions permission
WHERE role.code = 'OSD'
  AND permission.code IN (
      'DASHBOARD_VIEW_SELF', 'DASHBOARD_VIEW_SECTION',
      'DASHBOARD_VIEW_DEPARTMENT', 'DASHBOARD_VIEW_OFFICER',
      'DASHBOARD_VIEW_USER', 'DASHBOARD_VIEW_ORGANIZATION')
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Board members receive department/section drill-down but not unrestricted
-- arbitrary-user or organization scope by default.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), role.id, permission.id, NOW()
FROM identity_roles role
CROSS JOIN identity_permissions permission
WHERE role.code IN ('ADMINISTRATIVE_MEMBER', 'JUDICIAL_MEMBER')
  AND permission.code IN (
      'DASHBOARD_VIEW_SELF', 'DASHBOARD_VIEW_SECTION',
      'DASHBOARD_VIEW_DEPARTMENT')
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
ON CONFLICT (role_id, permission_id) DO NOTHING;
