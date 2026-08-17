package com.bor.eboard.workflow.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * workflow_tasks: a pending action assigned to a user/role/section.
 * Statuses: PENDING | COMPLETED | RETURNED | REASSIGNED | CANCELLED.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_tasks")
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workflow_instance_id", nullable = false)
    private UUID workflowInstanceId;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "letter_id")
    private UUID letterId;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "assigned_to_role_id")
    private UUID assignedToRoleId;

    @Column(name = "assigned_to_section_id")
    private UUID assignedToSectionId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
