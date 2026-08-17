package com.bor.eboard.checklist.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "letter_category_checklists")
public class LetterCategoryChecklist extends BaseEntity {
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    @Column(name = "checklist_template_id", nullable = false)
    private UUID checklistTemplateId;
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
