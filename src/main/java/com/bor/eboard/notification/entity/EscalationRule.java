package com.bor.eboard.notification.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * workflow_escalation_rules: a configurable rung of the escalation ladder.
 * The scheduler fires the highest rule whose days_after_due threshold a
 * task has crossed (03_DATABASE.md 10.7, 06_BUSINESS_RULES.md section 10).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_escalation_rules")
public class EscalationRule extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "days_after_due", nullable = false)
    private Integer daysAfterDue;

    @Column(name = "escalation_level", nullable = false, length = 50)
    private String escalationLevel;

    @Column(name = "notify_role_id")
    private UUID notifyRoleId;

    @Column(name = "notify_designation_id")
    private UUID notifyDesignationId;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
