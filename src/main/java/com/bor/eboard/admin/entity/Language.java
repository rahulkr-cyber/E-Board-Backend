package com.bor.eboard.admin.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * master_languages table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "master_languages")
public class Language extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
