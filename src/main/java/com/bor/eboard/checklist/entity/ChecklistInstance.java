package com.bor.eboard.checklist.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "checklist_instances")
public class ChecklistInstance extends BaseEntity {
    @Column(name = "checklist_template_id", nullable = false)
    private UUID checklistTemplateId;
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    @Column(name = "diary_entry_id")
    private UUID diaryEntryId;
    @Column(name = "letter_id")
    private UUID letterId;
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "forward_override_by")
    private UUID forwardOverrideBy;
    @Column(name = "forward_override_at")
    private LocalDateTime forwardOverrideAt;
    @Column(name = "forward_override_remarks")
    private String forwardOverrideRemarks;
}
