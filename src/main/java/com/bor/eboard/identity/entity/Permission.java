package com.bor.eboard.identity.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * identity_permissions table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "identity_permissions")
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 150)
    private String code;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    @Column(name = "description")
    private String description;
}
