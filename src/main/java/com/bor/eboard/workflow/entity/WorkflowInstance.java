package com.bor.eboard.workflow.entity;

import com.bor.eboard.common.util.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * workflow_instances: a running workflow for a particular file.
 * Statuses: ACTIVE | COMPLETED | REJECTED | CANCELLED.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workflow_template_id", nullable = false)
    private UUID workflowTemplateId;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "letter_id")
    private UUID letterId;

    @Column(name = "current_step_id")
    private UUID currentStepId;

    @Column(name = "current_step_order")
    private Integer currentStepOrder;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId().orElse(null);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
