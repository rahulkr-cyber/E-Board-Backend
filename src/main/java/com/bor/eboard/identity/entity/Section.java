package com.bor.eboard.identity.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * org_sections table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "org_sections")
public class Section extends BaseEntity {

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
