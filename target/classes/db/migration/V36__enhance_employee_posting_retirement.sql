-- =====================================================================
-- V36: Production-ready employee posting history and retirement support.
-- Retirement date is intentionally NOT stored; it is derived from DOB + 60.
-- =====================================================================

ALTER TABLE identity_users
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS date_of_joining DATE,
    ADD COLUMN IF NOT EXISTS profile_attachment_id UUID,
    ADD COLUMN IF NOT EXISTS district VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_identity_users_date_of_birth
    ON identity_users(date_of_birth);
CREATE INDEX IF NOT EXISTS idx_identity_users_district
    ON identity_users(district);

ALTER TABLE org_user_postings
    ADD COLUMN IF NOT EXISTS posting_type VARCHAR(50) DEFAULT 'INITIAL_POSTING',
    ADD COLUMN IF NOT EXISTS from_department_id UUID,
    ADD COLUMN IF NOT EXISTS from_section_id UUID,
    ADD COLUMN IF NOT EXISTS from_designation_id UUID,
    ADD COLUMN IF NOT EXISTS office_name VARCHAR(250),
    ADD COLUMN IF NOT EXISTS from_office_name VARCHAR(250),
    ADD COLUMN IF NOT EXISTS from_location VARCHAR(250),
    ADD COLUMN IF NOT EXISTS seat_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS reporting_officer_id UUID,
    ADD COLUMN IF NOT EXISTS controlling_officer_id UUID,
    ADD COLUMN IF NOT EXISTS location VARCHAR(250),
    ADD COLUMN IF NOT EXISTS district VARCHAR(150),
    ADD COLUMN IF NOT EXISTS from_district VARCHAR(150),
    ADD COLUMN IF NOT EXISTS transfer_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS permanent_charge BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS additional_charge BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS current_charge_holder_id UUID,
    ADD COLUMN IF NOT EXISTS previous_charge_holder_id UUID,
    ADD COLUMN IF NOT EXISTS charge_start_date DATE,
    ADD COLUMN IF NOT EXISTS charge_end_date DATE,
    ADD COLUMN IF NOT EXISTS charge_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS joining_date DATE,
    ADD COLUMN IF NOT EXISTS joining_time TIME,
    ADD COLUMN IF NOT EXISTS joining_order VARCHAR(100),
    ADD COLUMN IF NOT EXISTS joining_remarks TEXT,
    ADD COLUMN IF NOT EXISTS relieving_date DATE,
    ADD COLUMN IF NOT EXISTS relieving_time TIME,
    ADD COLUMN IF NOT EXISTS relieving_order VARCHAR(100),
    ADD COLUMN IF NOT EXISTS relieving_remarks TEXT,
    ADD COLUMN IF NOT EXISTS government_order_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS government_order_date DATE,
    ADD COLUMN IF NOT EXISTS attachment_id UUID,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'CURRENT',
    ADD COLUMN IF NOT EXISTS source_entity_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_entity_id UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Normalize pre-existing posting rows before enforcing the entity invariants.
UPDATE org_user_postings
SET posting_type = COALESCE(posting_type, 'INITIAL_POSTING'),
    permanent_charge = COALESCE(permanent_charge, TRUE),
    additional_charge = COALESCE(additional_charge, FALSE),
    active = COALESCE(active, posting_end_date IS NULL),
    status = CASE
        WHEN posting_end_date IS NULL AND COALESCE(active, TRUE) THEN 'CURRENT'
        ELSE 'PAST'
    END,
    deleted = COALESCE(deleted, FALSE);

ALTER TABLE org_user_postings
    ALTER COLUMN posting_type SET NOT NULL,
    ALTER COLUMN permanent_charge SET NOT NULL,
    ALTER COLUMN additional_charge SET NOT NULL,
    ALTER COLUMN active SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_org_user_postings_active
    ON org_user_postings(user_id, active, deleted);
CREATE INDEX IF NOT EXISTS idx_org_user_postings_dates
    ON org_user_postings(posting_start_date, posting_end_date);
CREATE INDEX IF NOT EXISTS idx_org_user_postings_source
    ON org_user_postings(source_entity_type, source_entity_id);
CREATE INDEX IF NOT EXISTS idx_org_user_postings_district
    ON org_user_postings(district);

ALTER TABLE org_transfer_history
    ADD COLUMN IF NOT EXISTS posting_type VARCHAR(50) DEFAULT 'TRANSFER',
    ADD COLUMN IF NOT EXISTS transfer_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS from_office_name VARCHAR(250),
    ADD COLUMN IF NOT EXISTS to_office_name VARCHAR(250),
    ADD COLUMN IF NOT EXISTS seat_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS reporting_officer_id UUID,
    ADD COLUMN IF NOT EXISTS controlling_officer_id UUID,
    ADD COLUMN IF NOT EXISTS from_location VARCHAR(250),
    ADD COLUMN IF NOT EXISTS to_location VARCHAR(250),
    ADD COLUMN IF NOT EXISTS from_district VARCHAR(150),
    ADD COLUMN IF NOT EXISTS to_district VARCHAR(150),
    ADD COLUMN IF NOT EXISTS government_order_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS government_order_date DATE,
    ADD COLUMN IF NOT EXISTS effective_to_date DATE,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE org_joining_relieving
    ADD COLUMN IF NOT EXISTS event_time TIME,
    ADD COLUMN IF NOT EXISTS government_order_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS government_order_date DATE,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
