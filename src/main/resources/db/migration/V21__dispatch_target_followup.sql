-- =====================================================================
-- V21 (BCR-03 Parts 9-10): Dispatch against a File OR a Letter, and
-- follow-up tracking fields on the existing followups table (no duplicate
-- dispatch_followup table — correspondence_followups is extended).
-- =====================================================================

ALTER TABLE correspondence_dispatch_register ADD COLUMN file_id UUID;
CREATE INDEX idx_dispatch_file ON correspondence_dispatch_register(file_id);

ALTER TABLE correspondence_followups ADD COLUMN followup_number VARCHAR(50);
ALTER TABLE correspondence_followups ADD COLUMN dispatch_id     UUID;
ALTER TABLE correspondence_followups ADD COLUMN due_date        DATE;
ALTER TABLE correspondence_followups ADD COLUMN reminder_date   DATE;
CREATE INDEX idx_followup_dispatch ON correspondence_followups(dispatch_id);
CREATE INDEX idx_followup_due      ON correspondence_followups(due_date);
