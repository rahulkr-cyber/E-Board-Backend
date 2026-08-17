package com.bor.eboard.identity.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * identity_roles table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "identity_roles")
public class Role extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
