package com.bor.eboard.notification.entity;

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
 * workflow_task_escalations: records that a given escalation level has
 * already fired for a task, preventing duplicate reminders for the same
 * level (06_BUSINESS_RULES.md section 10 rule 2).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_task_escalations")
public class TaskEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "file_id", updatable = false)
    private UUID fileId;

    @Column(name = "letter_id", updatable = false)
    private UUID letterId;

    @Column(name = "escalation_level", nullable = false, length = 50, updatable = false)
    private String escalationLevel;

    @Column(name = "days_after_due", nullable = false, updatable = false)
    private Integer daysAfterDue;

    @Column(name = "escalated_at", nullable = false, updatable = false)
    private LocalDateTime escalatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.escalatedAt == null) {
            this.escalatedAt = this.createdAt;
        }
    }
}
