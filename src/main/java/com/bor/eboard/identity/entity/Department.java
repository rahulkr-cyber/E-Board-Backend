package com.bor.eboard.identity.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * org_departments table. Owned by the Identity module
 * (02_ARCHITECTURE.md section 8).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "org_departments")
public class Department extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
