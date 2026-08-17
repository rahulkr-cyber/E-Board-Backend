package com.bor.eboard.assignment.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A configurable rule stating that a user holding {@code fromRoleId} may mark
 * work to users holding {@code toRoleId}. Section reach is governed by the
 * flags below. No routing logic is hardcoded in Java (BCR-03 Part 5/12):
 * the workflow decides the next ROLE, these rules decide the eligible USERS.
 */
@Entity
@Table(name = "assignment_rules")
@Getter
@Setter
@NoArgsConstructor
public class AssignmentRule extends BaseEntity {

    @Column(name = "from_role_id", nullable = false)
    private UUID fromRoleId;

    @Column(name = "to_role_id", nullable = false)
    private UUID toRoleId;

    /** NULL means the rule is not restricted to a single section. */
    @Column(name = "allowed_section_id")
    private UUID allowedSectionId;

    @Column(name = "same_section_allowed", nullable = false)
    private Boolean sameSectionAllowed = Boolean.TRUE;

    @Column(name = "cross_section_allowed", nullable = false)
    private Boolean crossSectionAllowed = Boolean.FALSE;

    @Column(name = "multi_user_allowed", nullable = false)
    private Boolean multiUserAllowed = Boolean.FALSE;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
