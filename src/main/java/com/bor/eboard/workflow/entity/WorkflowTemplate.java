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
 * workflow_templates: a reusable, DB-driven workflow definition.
 * Selection dimensions (category/department/section/priority) drive
 * template resolution (08_WORKFLOW_ENGINE.md section 3).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_templates")
public class WorkflowTemplate extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "priority_id")
    private UUID priorityId;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
