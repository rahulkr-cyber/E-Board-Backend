package com.bor.eboard.admin.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * master_letter_categories table.
 * default_workflow_template_id is a plain UUID reference resolved manually
 * once the Workflow module lands in Phase 5.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "master_letter_categories")
public class LetterCategory extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "default_workflow_template_id")
    private UUID defaultWorkflowTemplateId;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
