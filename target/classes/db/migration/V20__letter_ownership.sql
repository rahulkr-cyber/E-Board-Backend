-- =====================================================================
-- V20 (BCR-03 Part 16): Letter ownership trail.
-- NOTE: correspondence_letters already carries current_owner_id and
-- current_section_id (created in V5), so no columns are added here. What
-- was missing is (a) an owner index, (b) a backfill so existing letters
-- belong to their creator once box-scoped visibility switches on, and
-- (c) an immutable movement trail mirroring workflow_movements for files.
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_letters_current_owner
    ON correspondence_letters(current_owner_id);

-- Backfill: existing letters become owned by their creator, so nothing
-- disappears from anyone's working set when the new boxes go live.
UPDATE correspondence_letters
SET current_owner_id = created_by
WHERE current_owner_id IS NULL
  AND created_by IS NOT NULL
  AND deleted = FALSE;

CREATE TABLE correspondence_letter_movements (
    id               UUID PRIMARY KEY,
    letter_id        UUID NOT NULL,
    action           VARCHAR(30) NOT NULL,   -- MARK | RETURN | CLOSE | REOPEN
    from_user_id     UUID,
    from_section_id  UUID,
    to_user_id       UUID,
    to_section_id    UUID,
    remarks          VARCHAR(1000),
    action_at        TIMESTAMP NOT NULL,
    action_by        UUID NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP,
    created_by       UUID,
    updated_by       UUID,
    deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_letter_move_letter ON correspondence_letter_movements(letter_id);
CREATE INDEX idx_letter_move_to     ON correspondence_letter_movements(to_user_id);
CREATE INDEX idx_letter_move_from   ON correspondence_letter_movements(from_user_id);

-- Letter movements are immutable, exactly like workflow movements.
CREATE OR REPLACE FUNCTION prevent_letter_movement_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Letter movement records are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_letter_movements_immutable
BEFORE UPDATE OR DELETE ON correspondence_letter_movements
FOR EACH ROW EXECUTE FUNCTION prevent_letter_movement_change();
