ALTER TABLE correspondence_followups ALTER COLUMN file_id DROP NOT NULL;

ALTER TABLE correspondence_followups
    ADD CONSTRAINT ck_followup_business_subject
    CHECK (file_id IS NOT NULL OR letter_id IS NOT NULL OR dispatch_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_followups_letter
    ON correspondence_followups(letter_id) WHERE deleted = FALSE;
