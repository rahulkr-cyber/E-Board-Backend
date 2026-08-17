ALTER TABLE org_user_postings
    ADD COLUMN IF NOT EXISTS transfer_reason TEXT;

ALTER TABLE org_transfer_history
    ADD COLUMN IF NOT EXISTS transfer_reason TEXT;
