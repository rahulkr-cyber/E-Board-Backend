-- =====================================================================
-- V17 (BCR-03 Part 1): Official Board of Revenue organization hierarchy.
-- Designations gain a parent link; the official designation ladder and the
-- corresponding roles are seeded. Existing designations/roles stay active
-- for backward compatibility; only display names are aligned.
-- NOTE: this hierarchy is for organization management ONLY — the workflow
-- engine never follows it automatically (Part 12).
-- =====================================================================

ALTER TABLE org_designations ADD COLUMN parent_designation_id UUID;
CREATE INDEX idx_designation_parent ON org_designations(parent_designation_id);

-- Official designation ladder (new ids 30000000-...-0000000000 11..1a)
INSERT INTO org_designations (id, code, name, hierarchy_level, parent_designation_id, active, created_at, deleted) VALUES
('30000000-0000-0000-0000-000000000011', 'CHAIRMAN',            'Chairman',                   1, NULL, TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000012', 'ADMIN_MEMBER',        'Administrative Member',      2, '30000000-0000-0000-0000-000000000011', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000013', 'JUDICIAL_MEMBER',     'Judicial Member',            2, '30000000-0000-0000-0000-000000000011', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000014', 'COMMISSIONER_SECY',   'Commissioner & Secretary',   2, '30000000-0000-0000-0000-000000000011', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000015', 'OFFICER_ON_CAMP',     'Officer on Camp (OC)',       3, '30000000-0000-0000-0000-000000000014', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000016', 'OSD_NEW',             'Officer on Special Duty (OSD)', 3, '30000000-0000-0000-0000-000000000014', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000017', 'SO',                  'Section Officer (SO)',       4, '30000000-0000-0000-0000-000000000014', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000018', 'RO',                  'Review Officer (RO)',        5, '30000000-0000-0000-0000-000000000017', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-000000000019', 'ARO',                 'Assistant Review Officer (ARO)', 6, '30000000-0000-0000-0000-000000000018', TRUE, NOW(), FALSE),
('30000000-0000-0000-0000-00000000001a', 'COMPUTER_ASSISTANT',  'Computer Assistant',         7, '30000000-0000-0000-0000-000000000019', TRUE, NOW(), FALSE);

-- Registry remains an independent operational designation (no parent).
-- Existing REGISTRY_CLERK / DIARY_OPERATOR designations already model this.

-- New roles for the official actors (existing role codes untouched).
INSERT INTO identity_roles (id, code, name, description, active, created_at, deleted) VALUES
('40000000-0000-0000-0000-000000000011', 'ADMINISTRATIVE_MEMBER',    'Administrative Member',        'Member (Administration), Board of Revenue', TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000012', 'JUDICIAL_MEMBER',          'Judicial Member',              'Member (Judicial), Board of Revenue',       TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000013', 'OFFICER_ON_CAMP',          'Officer on Camp (OC)',         'Officer on Camp',                            TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000014', 'REVIEW_OFFICER',           'Review Officer (RO)',          'Review Officer',                             TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000015', 'ASSISTANT_REVIEW_OFFICER', 'Assistant Review Officer (ARO)','Assistant Review Officer',                  TRUE, NOW(), FALSE),
('40000000-0000-0000-0000-000000000016', 'COMPUTER_ASSISTANT',       'Computer Assistant',           'Computer Assistant',                         TRUE, NOW(), FALSE);

-- Align display names of apex roles with the official pattern (codes stable).
UPDATE identity_roles SET name = 'Chairman'                WHERE code = 'CHAIRPERSON'  AND deleted = FALSE;
UPDATE identity_roles SET name = 'Commissioner & Secretary' WHERE code = 'COMMISSIONER' AND deleted = FALSE;

-- Baseline grants for the new roles: view + notifications so they can work,
-- workflow view so files can reach them. Fine-grained grants follow in V22.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), r.id, p.id, NOW()
FROM identity_roles r
CROSS JOIN identity_permissions p
WHERE r.code IN ('ADMINISTRATIVE_MEMBER','JUDICIAL_MEMBER','OFFICER_ON_CAMP',
                 'REVIEW_OFFICER','ASSISTANT_REVIEW_OFFICER','COMPUTER_ASSISTANT')
  AND p.code IN ('NOTIFICATION_VIEW','DASHBOARD_VIEW','FILE_VIEW','LETTER_VIEW','DIARY_VIEW','WORKFLOW_VIEW')
  AND r.deleted = FALSE AND p.deleted = FALSE;
