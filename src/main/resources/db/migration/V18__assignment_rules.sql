-- =====================================================================
-- V18 (BCR-03 Part 5/12): Configurable assignment rules. The workflow
-- decides the NEXT ROLE; these DB rules decide WHICH USERS are eligible.
-- No routing logic is hardcoded in Java.
-- =====================================================================

CREATE TABLE assignment_rules (
    id                     UUID PRIMARY KEY,
    from_role_id           UUID NOT NULL,      -- role performing the mark
    to_role_id             UUID NOT NULL,      -- role that may receive
    allowed_section_id     UUID,               -- NULL = any section
    same_section_allowed   BOOLEAN NOT NULL DEFAULT TRUE,
    cross_section_allowed  BOOLEAN NOT NULL DEFAULT FALSE,
    multi_user_allowed     BOOLEAN NOT NULL DEFAULT FALSE,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    remarks                VARCHAR(500),
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP,
    created_by             UUID,
    updated_by             UUID,
    deleted                BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_assignrule_from_role ON assignment_rules(from_role_id, active);
CREATE INDEX idx_assignrule_to_role   ON assignment_rules(to_role_id, active);

-- Sensible defaults mirroring the official ladder, editable by the admin:
-- each level may mark one step up or down within the same section.
INSERT INTO assignment_rules (id, from_role_id, to_role_id, allowed_section_id,
        same_section_allowed, cross_section_allowed, multi_user_allowed, active, remarks, created_at, deleted)
SELECT gen_random_uuid(), f.id, t.id, NULL, TRUE, FALSE, FALSE, TRUE,
       'Default ladder rule (seeded by V18)', NOW(), FALSE
FROM identity_roles f
JOIN identity_roles t ON (
      (f.code = 'COMPUTER_ASSISTANT'        AND t.code = 'ASSISTANT_REVIEW_OFFICER')
   OR (f.code = 'ASSISTANT_REVIEW_OFFICER'  AND t.code IN ('REVIEW_OFFICER','COMPUTER_ASSISTANT'))
   OR (f.code = 'REVIEW_OFFICER'            AND t.code IN ('SECTION_OFFICER','ASSISTANT_REVIEW_OFFICER'))
   OR (f.code = 'SECTION_OFFICER'           AND t.code IN ('OSD','OFFICER_ON_CAMP','REVIEW_OFFICER'))
   OR (f.code = 'OSD'                       AND t.code IN ('COMMISSIONER','SECTION_OFFICER'))
   OR (f.code = 'OFFICER_ON_CAMP'           AND t.code IN ('COMMISSIONER','SECTION_OFFICER'))
   OR (f.code = 'COMMISSIONER'              AND t.code IN ('CHAIRPERSON','ADMINISTRATIVE_MEMBER','JUDICIAL_MEMBER','OSD','OFFICER_ON_CAMP'))
   OR (f.code = 'ADMINISTRATIVE_MEMBER'     AND t.code IN ('CHAIRPERSON','COMMISSIONER'))
   OR (f.code = 'JUDICIAL_MEMBER'           AND t.code IN ('CHAIRPERSON','COMMISSIONER'))
   OR (f.code = 'CHAIRPERSON'               AND t.code IN ('COMMISSIONER','ADMINISTRATIVE_MEMBER','JUDICIAL_MEMBER'))
   OR (f.code = 'REGISTRY_CLERK'            AND t.code IN ('SECTION_OFFICER','REVIEW_OFFICER','ASSISTANT_REVIEW_OFFICER'))
)
WHERE f.deleted = FALSE AND t.deleted = FALSE;

-- Cross-section marking allowed at leadership level by default.
UPDATE assignment_rules ar SET cross_section_allowed = TRUE
FROM identity_roles f
WHERE ar.from_role_id = f.id
  AND f.code IN ('CHAIRPERSON','COMMISSIONER','ADMINISTRATIVE_MEMBER','JUDICIAL_MEMBER','OSD','OFFICER_ON_CAMP');
