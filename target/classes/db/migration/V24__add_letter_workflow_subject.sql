ALTER TABLE workflow_instances ALTER COLUMN file_id DROP NOT NULL;
ALTER TABLE workflow_instances ADD COLUMN letter_id UUID;
ALTER TABLE workflow_instances ADD CONSTRAINT chk_workflow_instance_subject
    CHECK ((file_id IS NOT NULL AND letter_id IS NULL) OR (file_id IS NULL AND letter_id IS NOT NULL));
CREATE INDEX idx_workflow_instances_letter ON workflow_instances(letter_id);

ALTER TABLE workflow_tasks ALTER COLUMN file_id DROP NOT NULL;
ALTER TABLE workflow_tasks ADD COLUMN letter_id UUID;
ALTER TABLE workflow_tasks ADD CONSTRAINT chk_workflow_task_subject
    CHECK ((file_id IS NOT NULL AND letter_id IS NULL) OR (file_id IS NULL AND letter_id IS NOT NULL));
CREATE INDEX idx_workflow_tasks_letter ON workflow_tasks(letter_id);

ALTER TABLE workflow_movements ALTER COLUMN file_id DROP NOT NULL;
CREATE INDEX idx_workflow_movements_letter ON workflow_movements(letter_id);

ALTER TABLE workflow_task_escalations ALTER COLUMN file_id DROP NOT NULL;
ALTER TABLE workflow_task_escalations ADD COLUMN letter_id UUID;
ALTER TABLE workflow_task_escalations ADD CONSTRAINT chk_task_escalation_subject
    CHECK ((file_id IS NOT NULL AND letter_id IS NULL) OR (file_id IS NULL AND letter_id IS NOT NULL));
CREATE INDEX idx_task_escalations_letter ON workflow_task_escalations(letter_id);
