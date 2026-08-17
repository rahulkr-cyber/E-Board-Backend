-- =====================================================================
-- V23: Letter lifecycle permissions.
--
-- Purely additive: three new permission rows and their role grants. No table
-- is created or altered, and no existing row is modified. The letter entity
-- and the letter movement trail already supported this lifecycle
-- (LetterMovement declared MARK | RETURN | CLOSE | REOPEN from the start) —
-- only the permissions and endpoints were missing.
--
-- LETTER_MARK is reused for "return" (marking a letter back), since it is the
-- same act of handing a letter to another officer.
-- =====================================================================

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000081', 'LETTER_UPDATE', 'CORRESPONDENCE', 'Correct a draft letter''s metadata', NOW(), FALSE),
('50000000-0000-0000-0000-000000000082', 'LETTER_CLOSE',  'CORRESPONDENCE', 'Close a letter',                      NOW(), FALSE),
('50000000-0000-0000-0000-000000000083', 'LETTER_REOPEN', 'CORRESPONDENCE', 'Reopen a closed letter',              NOW(), FALSE);

-- SYSTEM_ADMIN: all three.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('LETTER_UPDATE', 'LETTER_CLOSE', 'LETTER_REOPEN')
  AND p.deleted = FALSE;

-- LETTER_UPDATE and LETTER_CLOSE: every role that already creates or works
-- letters. Correcting your own draft, and closing correspondence you hold, are
-- ordinary parts of the job.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE r.code IN ('CHAIRPERSON', 'COMMISSIONER', 'ADMINISTRATIVE_MEMBER', 'JUDICIAL_MEMBER',
                 'OSD', 'OFFICER_ON_CAMP', 'SECTION_OFFICER', 'ASSISTANT_SECTION_OFFICER',
                 'REVIEW_OFFICER', 'ASSISTANT_REVIEW_OFFICER', 'COMPUTER_ASSISTANT',
                 'REGISTRY_CLERK', 'DISPATCH_CLERK')
  AND p.code IN ('LETTER_UPDATE', 'LETTER_CLOSE')
  AND r.deleted = FALSE AND p.deleted = FALSE;

-- LETTER_REOPEN: restricted, mirroring FILE_REOPEN. Reopening closed
-- correspondence is a decision for the leadership, not the desk that closed it.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE r.code IN ('CHAIRPERSON', 'COMMISSIONER')
  AND p.code = 'LETTER_REOPEN'
  AND r.deleted = FALSE AND p.deleted = FALSE;
