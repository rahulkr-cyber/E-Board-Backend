package com.bor.eboard.workflow.entity;

import com.bor.eboard.common.util.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * workflow_movements: immutable file movement history
 * (08_WORKFLOW_ENGINE.md section 15). Insert-only — the database blocks
 * UPDATE and DELETE via trigger. No setters change persisted rows after
 * creation; there is no update lifecycle callback by design.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_movements")
public class WorkflowMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_id", updatable = false)
    private UUID fileId;

    @Column(name = "letter_id", updatable = false)
    private UUID letterId;

    @Column(name = "workflow_instance_id", updatable = false)
    private UUID workflowInstanceId;

    @Column(name = "from_user_id", updatable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", updatable = false)
    private UUID toUserId;

    @Column(name = "from_section_id", updatable = false)
    private UUID fromSectionId;

    @Column(name = "to_section_id", updatable = false)
    private UUID toSectionId;

    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "remarks", updatable = false)
    private String remarks;

    @Column(name = "charge_source_user_id", updatable = false)
    private UUID chargeSourceUserId;

    @Column(name = "charge_assignment_id", updatable = false)
    private UUID chargeAssignmentId;

    @Column(name = "ip_address", length = 100, updatable = false)
    private String ipAddress;

    @Column(name = "machine_name", length = 200, updatable = false)
    private String machineName;

    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.actionAt == null) {
            this.actionAt = this.createdAt;
        }
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId().orElse(null);
        }
    }
}
