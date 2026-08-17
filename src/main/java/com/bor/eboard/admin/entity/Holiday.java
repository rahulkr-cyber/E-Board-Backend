package com.bor.eboard.admin.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * master_holidays table. Used for SLA calculation (working days).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "master_holidays")
public class Holiday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "holiday_type", length = 50)
    private String holidayType;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
