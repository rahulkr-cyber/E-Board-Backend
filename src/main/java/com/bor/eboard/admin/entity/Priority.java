package com.bor.eboard.admin.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * master_priorities table. sla_days drives SLA calculation in later phases.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "master_priorities")
public class Priority extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "sla_days")
    private Integer slaDays;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
