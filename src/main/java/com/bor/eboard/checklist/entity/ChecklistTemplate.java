package com.bor.eboard.checklist.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "checklist_templates")
public class ChecklistTemplate extends BaseEntity {
    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "allow_forward_if_incomplete", nullable = false)
    private Boolean allowForwardIfIncomplete = Boolean.FALSE;
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
