-- =====================================================================
-- V38: Independent configurable Checklist Engine for Diary/Letter.
-- Reuses correspondence_attachments; no DMS table is referenced.
-- =====================================================================

CREATE TABLE checklist_templates (
    id                          UUID PRIMARY KEY,
    code                        VARCHAR(80) UNIQUE NOT NULL,
    name                        VARCHAR(200) NOT NULL,
    description                 TEXT,
    allow_forward_if_incomplete BOOLEAN NOT NULL DEFAULT FALSE,
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,
    created_by                  UUID,
    updated_by                  UUID,
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE checklist_items (
    id                         UUID PRIMARY KEY,
    checklist_template_id      UUID NOT NULL,
    item_name                  VARCHAR(200) NOT NULL,
    description                TEXT,
    mandatory                  BOOLEAN NOT NULL DEFAULT FALSE,
    active                     BOOLEAN NOT NULL DEFAULT TRUE,
    sequence_no                INT NOT NULL,
    remarks_required           BOOLEAN NOT NULL DEFAULT FALSE,
    allow_multiple_attachments BOOLEAN NOT NULL DEFAULT FALSE,
    supported_file_types       VARCHAR(500),
    maximum_file_size_bytes    BIGINT,
    created_at                 TIMESTAMP NOT NULL,
    updated_at                 TIMESTAMP,
    created_by                 UUID,
    updated_by                 UUID,
    deleted                    BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_checklist_items_template
    ON checklist_items(checklist_template_id, sequence_no);

CREATE TABLE letter_category_checklists (
    id                    UUID PRIMARY KEY,
    category_id           UUID NOT NULL,
    checklist_template_id UUID NOT NULL,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    created_by            UUID,
    updated_by            UUID,
    deleted               BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uq_active_category_checklist
    ON letter_category_checklists(category_id) WHERE deleted = FALSE;
CREATE INDEX idx_category_checklist_template
    ON letter_category_checklists(checklist_template_id);

CREATE TABLE checklist_instances (
    id                          UUID PRIMARY KEY,
    checklist_template_id       UUID NOT NULL,
    category_id                 UUID NOT NULL,
    diary_entry_id              UUID,
    letter_id                   UUID,
    status                      VARCHAR(50) NOT NULL,
    completed_at                TIMESTAMP,
    forward_override_by         UUID,
    forward_override_at         TIMESTAMP,
    forward_override_remarks    TEXT,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,
    created_by                  UUID,
    updated_by                  UUID,
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uq_checklist_instance_diary
    ON checklist_instances(diary_entry_id)
    WHERE diary_entry_id IS NOT NULL AND deleted = FALSE;
CREATE UNIQUE INDEX uq_checklist_instance_letter
    ON checklist_instances(letter_id)
    WHERE letter_id IS NOT NULL AND deleted = FALSE;
CREATE INDEX idx_checklist_instance_template ON checklist_instances(checklist_template_id);
CREATE INDEX idx_checklist_instance_status ON checklist_instances(status);

CREATE TABLE checklist_instance_items (
    id                         UUID PRIMARY KEY,
    checklist_instance_id      UUID NOT NULL,
    checklist_item_id          UUID,
    item_name                  VARCHAR(200) NOT NULL,
    description                TEXT,
    mandatory                  BOOLEAN NOT NULL DEFAULT FALSE,
    sequence_no                INT NOT NULL,
    remarks_required           BOOLEAN NOT NULL DEFAULT FALSE,
    allow_multiple_attachments BOOLEAN NOT NULL DEFAULT FALSE,
    supported_file_types       VARCHAR(500),
    maximum_file_size_bytes    BIGINT,
    status                     VARCHAR(50) NOT NULL,
    uploaded_by                UUID,
    uploaded_at                TIMESTAMP,
    verified_by                UUID,
    verified_at                TIMESTAMP,
    requested_by               UUID,
    requested_at               TIMESTAMP,
    remarks                    TEXT,
    active                     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                 TIMESTAMP NOT NULL,
    updated_at                 TIMESTAMP,
    created_by                 UUID,
    updated_by                 UUID,
    deleted                    BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_checklist_instance_items_instance
    ON checklist_instance_items(checklist_instance_id, sequence_no);
CREATE INDEX idx_checklist_instance_items_status
    ON checklist_instance_items(status);

ALTER TABLE correspondence_attachments
    ADD COLUMN checklist_instance_item_id UUID;
CREATE INDEX idx_attachments_checklist_item
    ON correspondence_attachments(checklist_instance_item_id)
    WHERE checklist_instance_item_id IS NOT NULL;

INSERT INTO identity_permissions (id, code, module, description, created_at, deleted) VALUES
('50000000-0000-0000-0000-000000000121', 'CHECKLIST_VIEW',     'CHECKLIST', 'View checklist configuration and case checklists', NOW(), FALSE),
('50000000-0000-0000-0000-000000000122', 'CHECKLIST_UPLOAD',   'CHECKLIST', 'Upload documents against checklist items', NOW(), FALSE),
('50000000-0000-0000-0000-000000000123', 'CHECKLIST_VERIFY',   'CHECKLIST', 'Verify, reject or request checklist documents', NOW(), FALSE),
('50000000-0000-0000-0000-000000000124', 'CHECKLIST_OVERRIDE', 'CHECKLIST', 'Forward an incomplete checklist with recorded justification', NOW(), FALSE),
('50000000-0000-0000-0000-000000000125', 'CHECKLIST_ADMIN',    'CHECKLIST', 'Configure checklist templates, items and category mappings', NOW(), FALSE)
ON CONFLICT (code) DO NOTHING;

-- Every role that already sees diaries or letters may see their checklist.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, p.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions existing ON existing.id = rp.permission_id
CROSS JOIN identity_permissions p
WHERE existing.code IN ('DIARY_VIEW', 'LETTER_VIEW')
  AND p.code = 'CHECKLIST_VIEW'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Registry/letter creators may upload checklist documents.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, p.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions existing ON existing.id = rp.permission_id
CROSS JOIN identity_permissions p
WHERE existing.code IN ('DIARY_CREATE', 'LETTER_CREATE')
  AND p.code = 'CHECKLIST_UPLOAD'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Workflow approvers may verify/request and override when policy permits.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, p.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions existing ON existing.id = rp.permission_id
CROSS JOIN identity_permissions p
WHERE existing.code = 'WORKFLOW_APPROVE'
  AND p.code IN ('CHECKLIST_VERIFY', 'CHECKLIST_OVERRIDE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Master administrators configure the engine.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), rp.role_id, p.id, NOW()
FROM identity_role_permissions rp
JOIN identity_permissions existing ON existing.id = rp.permission_id
CROSS JOIN identity_permissions p
WHERE existing.code = 'MASTER_MANAGE'
  AND p.code = 'CHECKLIST_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SYSTEM_ADMIN gets all checklist permissions even if prior role grants differ.
INSERT INTO identity_role_permissions (id, role_id, permission_id, created_at)
SELECT gen_random_uuid(), '40000000-0000-0000-0000-000000000001', p.id, NOW()
FROM identity_permissions p
WHERE p.code IN ('CHECKLIST_VIEW', 'CHECKLIST_UPLOAD', 'CHECKLIST_VERIFY', 'CHECKLIST_OVERRIDE', 'CHECKLIST_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;
