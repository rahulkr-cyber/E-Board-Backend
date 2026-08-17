package com.bor.eboard.workflow.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * workflow_steps: one approval/movement level inside a template.
 * A step targets a role, designation, section and/or a specific user;
 * the engine resolves the concrete assignee at runtime.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_steps")
public class WorkflowStep extends BaseEntity {

    @Column(name = "workflow_template_id", nullable = false)
    private UUID workflowTemplateId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", nullable = false, length = 200)
    private String stepName;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "designation_id")
    private UUID designationId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "specific_user_id")
    private UUID specificUserId;

    @Column(name = "approval_required")
    private Boolean approvalRequired = Boolean.TRUE;

    @Column(name = "can_return")
    private Boolean canReturn = Boolean.TRUE;

    @Column(name = "can_reassign")
    private Boolean canReassign = Boolean.TRUE;

    @Column(name = "can_reject")
    private Boolean canReject = Boolean.TRUE;

    @Column(name = "sla_days")
    private Integer slaDays;

    @Column(name = "parallel_step")
    private Boolean parallelStep = Boolean.FALSE;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
