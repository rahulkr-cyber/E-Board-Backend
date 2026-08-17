-- =====================================================================
-- V12: Seed data - admin user, roles, permissions, departments,
--      sections, designations, role-permission and user-role mappings
-- Default admin credentials: admin / Admin@123  (change in production)
-- =====================================================================

-- ---------------------------------------------------------------
-- Departments
-- ---------------------------------------------------------------
INSERT INTO org_departments (id, code, name, description, active, created_at, deleted) VALUES
('10000000-0000-0000-0000-000000000001', 'BOR', 'Board of Revenue', 'Board of Revenue, Government of Uttar Pradesh', TRUE, NOW(), FALSE);

-- ---------------------------------------------------------------
-- Sections
-- ---------------------------------------------------------------
INSERT INTO org_sections (id, department_id, code, name, description, active, created_at, deleted) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'REVENUE',        'Revenue',         'Revenue Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'JUDICIAL',       'Judicial',        'Judicial Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'LAND_RECORDS',   'Land Records',    'Land Records Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'LEGAL',          'Legal',           'Legal Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'FINANCE',        'Finance',         'Finance Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'ACCOUNTS',       'Accounts',        'Accounts Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'ADMINISTRATION', 'Administration',  'Administration Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'ESTABLISHMENT',  'Establishment',   'Establishment Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', 'CONFIDENTIAL',   'Confidential',    'Confidential Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-000000000001', 'IT',             'IT',              'IT Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-000000000001', 'REGISTRY',       'Registry',        'Registry Section', TRUE, NOW(), FALSE),
('20000000-0000-0000-0000-00000000000c', '10000000-0000-0000-0000-000000000001', 'RECORD_ROOM',    'Record Room',     'Record Room Section', TRUE, NOW(), FALSE);

-- ---------------------------------------------------------------
-- Designations
-- ---------------------------------------------------------------
INSERT INTO org_designations (id, code, name, hierarchy_level, active, created_at, deleted) VALUES
('30000000-0000-0000-0000-000000000001', 'CHAIRPERSON',    'Chairperson',               1, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000002', 'COMMISSIONER',   'Commissioner',              2, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000003', 'OSD',            'Officer on Special Duty',   3, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000004', 'SECTION_OFFICER','Section Officer',           4, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000005', 'ASO',            'Assistant Section Officer', 5, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000006', 'REGISTRY_CLERK', 'Registry Clerk',            6, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000007', 'DIARY_OPERATOR', 'Diary Operator',            7, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000008', 'DISPATCH_CLERK', 'Dispatch Clerk',            7, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000009', 'NIC_SD_IT',      'NIC SD-IT',                 5, TRUE, NOW(), FALSE);

-- ---------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------
INSERT INTO identity_roles (id, code, name, description, active, created_at, deleted) VALUES
('40000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN',             'System Administrator',       'Full system administration', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000002', 'CHAIRPERSON',              'Chairperson',                'Chairperson of the Board', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000003', 'COMMISSIONER',             'Commissioner',               'Commissioner / Member of the Board', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000004', 'OSD',                      'Officer on Special Duty',    'OSD', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000005', 'SECTION_OFFICER',          'Section Officer',            'Section Officer', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000006', 'ASSISTANT_SECTION_OFFICER','Assistant Section Officer',  'Assistant Section Officer / Dealing Hand', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000007', 'REGISTRY_CLERK',           'Registry Clerk',             'Registry intake clerk', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000008', 'DIARY_OPERATOR',           'Diary Operator',             'Diary entry operator', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000009', 'DISPATCH_CLERK',           'Dispatch Clerk',             'Outward dispatch clerk', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-00000000000a', 'RECORD_ROOM',              'Record Room',                'Record room / archival staff', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-00000000000b', 'NIC_SD_IT',                'NIC SD-IT',                  'NIC / IT support', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-00000000000c', 'READ_ONLY',                'Read Only',                  'Read-only access', TRUE, NOW(), FALSE);

-- ---------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------
INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
-- Identity / users
('50000000-0000-0000-0000-000000000001', 'USER_CREATE',        'IDENTITY', 'Create user', NOW(), FALSE),
('50000000-0000-0000-0000-000000000002', 'USER_UPDATE',        'IDENTITY', 'Update user', NOW(), FALSE),
('50000000-0000-0000-0000-000000000003', 'USER_VIEW',          'IDENTITY', 'View users', NOW(), FALSE),
('50000000-0000-0000-0000-000000000004', 'USER_STATUS_CHANGE', 'IDENTITY', 'Activate/suspend/lock users', NOW(), FALSE),
('50000000-0000-0000-0000-000000000005', 'USER_ROLE_ASSIGN',   'IDENTITY', 'Assign roles to users', NOW(), FALSE),
-- Roles / permissions
('50000000-0000-0000-0000-000000000006', 'ROLE_CREATE',        'IDENTITY', 'Create role', NOW(), FALSE),
('50000000-0000-0000-0000-000000000007', 'ROLE_UPDATE',        'IDENTITY', 'Update role', NOW(), FALSE),
('50000000-0000-0000-0000-000000000008', 'ROLE_VIEW',          'IDENTITY', 'View roles', NOW(), FALSE),
('50000000-0000-0000-0000-000000000009', 'ROLE_PERMISSION_ASSIGN', 'IDENTITY', 'Assign permissions to roles', NOW(), FALSE),
('50000000-0000-0000-0000-00000000000a', 'PERMISSION_VIEW',    'IDENTITY', 'View permissions', NOW(), FALSE),
-- Organization
('50000000-0000-0000-0000-00000000000b', 'ORG_CREATE',         'ORGANIZATION', 'Create department/section/designation', NOW(), FALSE),
('50000000-0000-0000-0000-00000000000c', 'ORG_UPDATE',         'ORGANIZATION', 'Update department/section/designation', NOW(), FALSE),
('50000000-0000-0000-0000-00000000000d', 'ORG_VIEW',           'ORGANIZATION', 'View organization masters', NOW(), FALSE),
-- Registry (used from Phase 3 onward, seeded now for RBAC completeness)
('50000000-0000-0000-0000-00000000000e', 'DIARY_CREATE',       'REGISTRY', 'Create diary entry', NOW(), FALSE),
('50000000-0000-0000-0000-00000000000f', 'DIARY_VIEW',         'REGISTRY', 'View diary entries', NOW(), FALSE),
('50000000-0000-0000-0000-000000000010', 'DIARY_FORWARD',      'REGISTRY', 'Forward diary entry to section', NOW(), FALSE),
-- Correspondence
('50000000-0000-0000-0000-000000000011', 'LETTER_CREATE',      'CORRESPONDENCE', 'Create letter', NOW(), FALSE),
('50000000-0000-0000-0000-000000000012', 'LETTER_VIEW',        'CORRESPONDENCE', 'View letters', NOW(), FALSE),
('50000000-0000-0000-0000-000000000013', 'LETTER_FORWARD',     'CORRESPONDENCE', 'Forward letter', NOW(), FALSE),
('50000000-0000-0000-0000-000000000014', 'FILE_CREATE',        'CORRESPONDENCE', 'Create file', NOW(), FALSE),
('50000000-0000-0000-0000-000000000015', 'FILE_VIEW',          'CORRESPONDENCE', 'View files', NOW(), FALSE),
('50000000-0000-0000-0000-000000000016', 'FILE_FORWARD',       'CORRESPONDENCE', 'Forward file', NOW(), FALSE),
('50000000-0000-0000-0000-000000000017', 'FILE_CLOSE',         'CORRESPONDENCE', 'Close file', NOW(), FALSE),
-- Workflow
('50000000-0000-0000-0000-000000000018', 'WORKFLOW_APPROVE',   'WORKFLOW', 'Approve workflow step', NOW(), FALSE),
('50000000-0000-0000-0000-000000000019', 'WORKFLOW_REJECT',    'WORKFLOW', 'Reject workflow step', NOW(), FALSE),
('50000000-0000-0000-0000-00000000001a', 'WORKFLOW_RETURN',    'WORKFLOW', 'Return file for clarification', NOW(), FALSE),
('50000000-0000-0000-0000-00000000001b', 'WORKFLOW_REASSIGN',  'WORKFLOW', 'Reassign workflow task', NOW(), FALSE),
('50000000-0000-0000-0000-00000000001c', 'WORKFLOW_CONFIGURE', 'WORKFLOW', 'Configure workflow templates', NOW(), FALSE),
-- Reports / audit / admin
('50000000-0000-0000-0000-00000000001d', 'REPORT_VIEW',        'REPORTS', 'View reports', NOW(), FALSE),
('50000000-0000-0000-0000-00000000001e', 'REPORT_EXPORT',      'REPORTS', 'Export reports', NOW(), FALSE),
('50000000-0000-0000-0000-00000000001f', 'AUDIT_VIEW',         'AUDIT', 'View audit logs', NOW(), FALSE),
('50000000-0000-0000-0000-000000000020', 'MASTER_MANAGE',      'ADMIN', 'Manage master data', NOW(), FALSE),
('50000000-0000-0000-0000-000000000021', 'CHARGE_MANAGE',      'ADMIN', 'Manage charge assignments', NOW(), FALSE),
('50000000-0000-0000-0000-000000000022', 'DASHBOARD_VIEW',     'DASHBOARD', 'View dashboards', NOW(), FALSE);

-- ---------------------------------------------------------------
-- Role-permission mappings
-- ---------------------------------------------------------------
-- SYSTEM_ADMIN gets everything
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CHAIRPERSON
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000002', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('USER_VIEW','ROLE_VIEW','ORG_VIEW','DIARY_VIEW','LETTER_VIEW','FILE_VIEW',
                 'WORKFLOW_APPROVE','WORKFLOW_REJECT','WORKFLOW_RETURN',
                 'REPORT_VIEW','REPORT_EXPORT','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- COMMISSIONER
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000003', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('USER_VIEW','ORG_VIEW','DIARY_VIEW','LETTER_VIEW','FILE_VIEW',
                 'WORKFLOW_APPROVE','WORKFLOW_REJECT','WORKFLOW_RETURN','WORKFLOW_REASSIGN',
                 'REPORT_VIEW','REPORT_EXPORT','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- OSD
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000004', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DIARY_VIEW','LETTER_VIEW','FILE_VIEW','FILE_FORWARD',
                 'WORKFLOW_APPROVE','WORKFLOW_RETURN','WORKFLOW_REASSIGN',
                 'REPORT_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SECTION_OFFICER
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000005', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DIARY_VIEW','LETTER_CREATE','LETTER_VIEW','LETTER_FORWARD',
                 'FILE_CREATE','FILE_VIEW','FILE_FORWARD','FILE_CLOSE',
                 'WORKFLOW_APPROVE','WORKFLOW_RETURN','WORKFLOW_REASSIGN',
                 'REPORT_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ASSISTANT_SECTION_OFFICER
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000006', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DIARY_VIEW','LETTER_CREATE','LETTER_VIEW','LETTER_FORWARD',
                 'FILE_VIEW','FILE_FORWARD','WORKFLOW_RETURN',
                 'REPORT_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- REGISTRY_CLERK
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000007', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DIARY_CREATE','DIARY_VIEW','DIARY_FORWARD','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DIARY_OPERATOR
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000008', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DIARY_CREATE','DIARY_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DISPATCH_CLERK
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000009', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('LETTER_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECORD_ROOM
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-00000000000a', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('FILE_VIEW','LETTER_VIEW','DIARY_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- NIC_SD_IT
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-00000000000b', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('USER_VIEW','ROLE_VIEW','ORG_VIEW','AUDIT_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- READ_ONLY
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-00000000000c', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('DIARY_VIEW','LETTER_VIEW','FILE_VIEW','REPORT_VIEW','DASHBOARD_VIEW') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------
-- Admin user  (username: admin / password: Admin@123)
-- ---------------------------------------------------------------
INSERT INTO identity_users
(id, employee_code, username, full_name, email, mobile, password_hash, status,
 department_id, section_id, designation_id,
 failed_login_attempts, account_locked, password_changed_at, created_at, deleted)
VALUES
('60000000-0000-0000-0000-000000000001', 'BOR-ADMIN-001', 'admin', 'System Administrator',
 'admin@bor.up.gov.in', '9999999999',
 '$2b$10$Kwbss3apsMja1hlo3KhKz.413hEXjM/5tZ8loNJkzJfT1CHfCKq.i',
 'ACTIVE',
 '10000000-0000-0000-0000-000000000001',
 '20000000-0000-0000-0000-00000000000a',
 '30000000-0000-0000-0000-000000000009',
 0, FALSE, NOW(), NOW(), FALSE);

INSERT INTO identity_user_roles (id, user_id, role_id, active, effective_from, created_at)
VALUES (gen_random_uuid(), '60000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001', TRUE, CURRENT_DATE, NOW());
