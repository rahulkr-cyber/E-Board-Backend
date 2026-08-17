-- =====================================================================
-- V39: Extend the independent Checklist Engine into a metadata-driven
-- dynamic form builder. Existing document checklist items remain valid.
-- No DMS table or storage path is referenced.
-- =====================================================================

ALTER TABLE checklist_items
    ADD COLUMN control_type VARCHAR(40) NOT NULL DEFAULT 'DOCUMENT_UPLOAD',
    ADD COLUMN display_label VARCHAR(200),
    ADD COLUMN placeholder VARCHAR(300),
    ADD COLUMN help_text TEXT,
    ADD COLUMN read_only BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN default_value_json TEXT,
    ADD COLUMN validation_rules_json TEXT,
    ADD COLUMN minimum_length INT,
    ADD COLUMN maximum_length INT,
    ADD COLUMN regex_pattern VARCHAR(1000),
    ADD COLUMN options_json TEXT,
    ADD COLUMN auto_value_expression VARCHAR(1000),
    ADD COLUMN parent_item_sequence INT,
    ADD COLUMN visibility_condition_operator VARCHAR(40),
    ADD COLUMN visibility_condition_value TEXT,
    ADD COLUMN mandatory_condition_operator VARCHAR(40),
    ADD COLUMN mandatory_condition_value TEXT;

ALTER TABLE checklist_instance_items
    ADD COLUMN control_type VARCHAR(40) NOT NULL DEFAULT 'DOCUMENT_UPLOAD',
    ADD COLUMN display_label VARCHAR(200),
    ADD COLUMN placeholder VARCHAR(300),
    ADD COLUMN help_text TEXT,
    ADD COLUMN read_only BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN default_value_json TEXT,
    ADD COLUMN validation_rules_json TEXT,
    ADD COLUMN minimum_length INT,
    ADD COLUMN maximum_length INT,
    ADD COLUMN regex_pattern VARCHAR(1000),
    ADD COLUMN options_json TEXT,
    ADD COLUMN auto_value_expression VARCHAR(1000),
    ADD COLUMN parent_item_sequence INT,
    ADD COLUMN visibility_condition_operator VARCHAR(40),
    ADD COLUMN visibility_condition_value TEXT,
    ADD COLUMN mandatory_condition_operator VARCHAR(40),
    ADD COLUMN mandatory_condition_value TEXT,
    ADD COLUMN response_value_json TEXT,
    ADD COLUMN response_display_value TEXT,
    ADD COLUMN responded_by UUID,
    ADD COLUMN responded_at TIMESTAMP;

UPDATE checklist_items
SET display_label = item_name
WHERE display_label IS NULL;

UPDATE checklist_instance_items
SET display_label = item_name
WHERE display_label IS NULL;

CREATE INDEX idx_checklist_items_parent_sequence
    ON checklist_items(checklist_template_id, parent_item_sequence)
    WHERE parent_item_sequence IS NOT NULL AND deleted = FALSE;

CREATE INDEX idx_checklist_instance_items_parent_sequence
    ON checklist_instance_items(checklist_instance_id, parent_item_sequence)
    WHERE parent_item_sequence IS NOT NULL AND deleted = FALSE;
