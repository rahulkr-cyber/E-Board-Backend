-- Targeted indexes for the existing DMS advanced-search filters.
-- No table or API contract is replaced.

CREATE INDEX IF NOT EXISTS idx_dms_search_department_date
    ON dms_search_index (department_id, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_dms_search_section_date
    ON dms_search_index (section_id, uploaded_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_dms_search_modified_date
    ON dms_search_index ((COALESCE(document_updated_at, uploaded_at)) DESC)
    WHERE deleted = FALSE;
