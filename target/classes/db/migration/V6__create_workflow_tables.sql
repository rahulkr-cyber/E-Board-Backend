-- =====================================================================
-- V6: Workflow engine tables (03_DATABASE.md section 10)
-- templates, steps, instances, tasks, and immutable movement history.
-- No approval chain is hardcoded: chains live entirely in these tables
-- (08_WORKFLOW_ENGINE.md section 1).
-- =====================================================================

CREATE TABLE workflow_templates (
    id            UUID PRIMARY KEY,
    code          VARCHAR(100) UNIQUE NOT NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    category_id   UUID,
    department_id UUID,
    section_id    UUID,
    priority_id   UUID,
    active        BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP,
    created_by    UUID,
    updated_by    UUID,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_workflow_templates_selection
    ON workflow_templates(category_id, department_id, section_id, priority_id);

CREATE TABLE workflow_steps (
    id                   UUID PRIMARY KEY,
    workflow_template_id UUID NOT NULL,
    step_order           INT NOT NULL,
    step_name            VARCHAR(200) NOT NULL,
    role_id              UUID,
    designation_id       UUID,
    section_id           UUID,
    specific_user_id     UUID,
    approval_required    BOOLEAN DEFAULT TRUE,
    can_return           BOOLEAN DEFAULT TRUE,
    can_reassign         BOOLEAN DEFAULT TRUE,
    can_reject           BOOLEAN DEFAULT TRUE,
    sla_days             INT,
    parallel_step        BOOLEAN DEFAULT FALSE,
    active               BOOLEAN DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP,
    created_by           UUID,
    updated_by           UUID,
    deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_workflow_steps_order UNIQUE (workflow_template_id, step_order)
);

CREATE INDEX idx_workflow_steps_template ON workflow_steps(workflow_template_id);

CREATE TABLE workflow_instances (
    id                   UUID PRIMARY KEY,
    workflow_template_id UUID NOT NULL,
    file_id              UUID NOT NULL,
    current_step_id      UUID,
    current_step_order   INT,
    status               VARCHAR(50) NOT NULL,
    started_at           TIMESTAMP NOT NULL,
    completed_at         TIMESTAMP,
    created_by           UUID,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP
);

CREATE INDEX idx_workflow_instances_file ON workflow_instances(file_id);
CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);

CREATE TABLE workflow_tasks (
    id                    UUID PRIMARY KEY,
    workflow_instance_id  UUID NOT NULL,
    file_id               UUID NOT NULL,
    step_id               UUID NOT NULL,
    assigned_to_user_id   UUID,
    assigned_to_role_id   UUID,
    assigned_to_section_id UUID,
    status                VARCHAR(50) NOT NULL,
    due_date              DATE,
    assigned_at           TIMESTAMP NOT NULL,
    completed_at          TIMESTAMP,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP
);

CREATE INDEX idx_workflow_tasks_instance ON workflow_tasks(workflow_instance_id);
CREATE INDEX idx_workflow_tasks_file ON workflow_tasks(file_id);
CREATE INDEX idx_workflow_tasks_user ON workflow_tasks(assigned_to_user_id);
CREATE INDEX idx_workflow_tasks_role ON workflow_tasks(assigned_to_role_id);
CREATE INDEX idx_workflow_tasks_section ON workflow_tasks(assigned_to_section_id);
CREATE INDEX idx_workflow_tasks_status ON workflow_tasks(status);

-- Immutable movement history (08_WORKFLOW_ENGINE.md section 15).
CREATE TABLE workflow_movements (
    id                   UUID PRIMARY KEY,
    file_id              UUID NOT NULL,
    letter_id            UUID,
    workflow_instance_id UUID,
    from_user_id         UUID,
    to_user_id           UUID,
    from_section_id      UUID,
    to_section_id        UUID,
    action               VARCHAR(50) NOT NULL,
    remarks              TEXT,
    charge_source_user_id UUID,
    charge_assignment_id  UUID,
    ip_address           VARCHAR(100),
    machine_name         VARCHAR(200),
    action_at            TIMESTAMP NOT NULL,
    created_by           UUID,
    created_at           TIMESTAMP NOT NULL
);

CREATE INDEX idx_workflow_movements_file ON workflow_movements(file_id);
CREATE INDEX idx_workflow_movements_instance ON workflow_movements(workflow_instance_id);

-- Enforce immutability of movement records at the database level:
-- no UPDATE or DELETE is ever permitted (mirrors the audit_logs guard).
CREATE OR REPLACE FUNCTION trg_block_workflow_movement_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'workflow_movements is immutable: % not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER block_workflow_movement_update
    BEFORE UPDATE ON workflow_movements
    FOR EACH ROW EXECUTE FUNCTION trg_block_workflow_movement_mutation();

CREATE TRIGGER block_workflow_movement_delete
    BEFORE DELETE ON workflow_movements
    FOR EACH ROW EXECUTE FUNCTION trg_block_workflow_movement_mutation();
