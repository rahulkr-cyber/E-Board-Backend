package com.bor.eboard.charge.entity;

import com.bor.eboard.common.util.SecurityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * org_transfer_history: a permanent, insert-only record of a user's
 * transfer between departments/sections/designations (03_DATABASE.md 5.5).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "org_transfer_history")
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "from_department_id", updatable = false)
    private UUID fromDepartmentId;

    @Column(name = "from_section_id", updatable = false)
    private UUID fromSectionId;

    @Column(name = "to_department_id", nullable = false, updatable = false)
    private UUID toDepartmentId;

    @Column(name = "to_section_id", nullable = false, updatable = false)
    private UUID toSectionId;

    @Column(name = "from_designation_id", updatable = false)
    private UUID fromDesignationId;

    @Column(name = "to_designation_id", updatable = false)
    private UUID toDesignationId;

    @Column(name = "posting_type", length = 50, updatable = false)
    private String postingType;

    @Column(name = "transfer_type", length = 50, updatable = false)
    private String transferType;

    @Column(name = "from_office_name", length = 250, updatable = false)
    private String fromOfficeName;

    @Column(name = "to_office_name", length = 250, updatable = false)
    private String toOfficeName;

    @Column(name = "seat_name", length = 150, updatable = false)
    private String seatName;

    @Column(name = "reporting_officer_id", updatable = false)
    private UUID reportingOfficerId;

    @Column(name = "controlling_officer_id", updatable = false)
    private UUID controllingOfficerId;

    @Column(name = "from_location", length = 250, updatable = false)
    private String fromLocation;

    @Column(name = "to_location", length = 250, updatable = false)
    private String toLocation;

    @Column(name = "from_district", length = 150, updatable = false)
    private String fromDistrict;

    @Column(name = "to_district", length = 150, updatable = false)
    private String toDistrict;

    @Column(name = "transfer_date", nullable = false, updatable = false)
    private LocalDate transferDate;

    @Column(name = "effective_to_date", updatable = false)
    private LocalDate effectiveToDate;

    @Column(name = "order_number", length = 100, updatable = false)
    private String orderNumber;

    @Column(name = "order_date", updatable = false)
    private LocalDate orderDate;

    @Column(name = "government_order_number", length = 100, updatable = false)
    private String governmentOrderNumber;

    @Column(name = "government_order_date", updatable = false)
    private LocalDate governmentOrderDate;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Column(name = "transfer_reason", updatable = false)
    private String transferReason;

    @Column(name = "remarks", updatable = false)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId().orElse(null);
        }
    }
}
