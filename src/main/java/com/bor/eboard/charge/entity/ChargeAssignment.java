package com.bor.eboard.charge.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * org_charge_assignments: a temporary/additional/full charge grant from one
 * user to another (03_DATABASE.md 5.7). While ACTIVE and within its
 * effective window, it lets the to-user access the from-user's pending work
 * (12_BUSINESS_RULES.md section 12 rule 6).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "org_charge_assignments")
public class ChargeAssignment extends BaseEntity {

    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @Column(name = "charge_type", nullable = false, length = 50)
    private String chargeType;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "order_number", length = 100)
    private String orderNumber;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
