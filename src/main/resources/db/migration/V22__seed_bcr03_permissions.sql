-- =====================================================================
-- V22 (BCR-03): New permissions and grants.
-- Priorities LOW/NORMAL/HIGH/URGENT already exist (V13).
-- =====================================================================

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000071', 'FILE_MARK',              'CORRESPONDENCE', 'Mark a file to a section/role/user',      NOW(), FALSE),
('50000000-0000-0000-0000-000000000072', 'FILE_PRIORITY_CHANGE',   'CORRESPONDENCE', 'Change file priority',                    NOW(), FALSE),
('50000000-0000-0000-0000-000000000073', 'FILE_REOPEN',            'CORRESPONDENCE', 'Reopen a closed file',                    NOW(), FALSE),
('50000000-0000-0000-0000-000000000074', 'LETTER_MARK',            'CORRESPONDENCE', 'Mark a letter to a section/role/user',    NOW(), FALSE),
('50000000-0000-0000-0000-000000000075', 'ASSIGNMENT_RULE_MANAGE', 'ADMIN',          'Configure marking/assignment rules',      NOW(), FALSE),
('50000000-0000-0000-0000-000000000076', 'REGISTER_SECTION_VIEW',  'REPORTS',        'View the section file register',          NOW(), FALSE),
('50000000-0000-0000-0000-000000000077', 'REGISTER_CENTRAL_VIEW',  'REPORTS',        'View the central file register',          NOW(), FALSE);

-- SYSTEM_ADMIN: everything new.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('FILE_MARK','FILE_PRIORITY_CHANGE','FILE_REOPEN','LETTER_MARK',
                 'ASSIGNMENT_RULE_MANAGE','REGISTER_SECTION_VIEW','REGISTER_CENTRAL_VIEW')
  AND p.deleted = FALSE;

-- Priority change + reopen: Chairman and Commissioner & Secretary only (Part 7-8).
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r CROSS JOIN identity_permissions p
WHERE r.code IN ('CHAIRPERSON','COMMISSIONER')
  AND p.code IN ('FILE_PRIORITY_CHANGE','FILE_REOPEN','REGISTER_CENTRAL_VIEW')
  AND r.deleted = FALSE AND p.deleted = FALSE;

-- Marking: every file-working role.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r CROSS JOIN identity_permissions p
WHERE r.code IN ('CHAIRPERSON','COMMISSIONER','ADMINISTRATIVE_MEMBER','JUDICIAL_MEMBER',
                 'OSD','OFFICER_ON_CAMP','SECTION_OFFICER','ASSISTANT_SECTION_OFFICER',
                 'REVIEW_OFFICER','ASSISTANT_REVIEW_OFFICER','COMPUTER_ASSISTANT','REGISTRY_CLERK')
  AND p.code IN ('FILE_MARK','LETTER_MARK')
  AND r.deleted = FALSE AND p.deleted = FALSE;

-- FILE_CREATE for every authorized creator listed in Part 2.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r CROSS JOIN identity_permissions p
WHERE r.code IN ('CHAIRPERSON','COMMISSIONER','ADMINISTRATIVE_MEMBER','JUDICIAL_MEMBER',
                 'OSD','OFFICER_ON_CAMP','REVIEW_OFFICER','ASSISTANT_REVIEW_OFFICER','COMPUTER_ASSISTANT')
  AND p.code = 'FILE_CREATE'
  AND r.deleted = FALSE AND p.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1 FROM identity_role_permissions x
      WHERE x.role_id = r.id AND x.permission_id = p.id);

-- Section register: section-level working roles.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r CROSS JOIN identity_permissions p
WHERE r.code IN ('SECTION_OFFICER','REVIEW_OFFICER','ASSISTANT_REVIEW_OFFICER',
                 'OSD','OFFICER_ON_CAMP','ASSISTANT_SECTION_OFFICER')
  AND p.code = 'REGISTER_SECTION_VIEW'
  AND r.deleted = FALSE AND p.deleted = FALSE;
