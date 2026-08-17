package com.bor.eboard.charge.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Detailed, insert-only employee service posting held in the existing
 * org_user_postings table. Current identity columns remain on identity_users;
 * this table preserves every substantive posting and its joining/relieving
 * metadata without overwriting history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "org_user_postings")
public class UserPosting extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "department_id", nullable = false, updatable = false)
    private UUID departmentId;

    @Column(name = "section_id", nullable = false, updatable = false)
    private UUID sectionId;

    @Column(name = "designation_id", nullable = false, updatable = false)
    private UUID designationId;

    @Column(name = "from_department_id", updatable = false)
    private UUID fromDepartmentId;

    @Column(name = "from_section_id", updatable = false)
    private UUID fromSectionId;

    @Column(name = "from_designation_id", updatable = false)
    private UUID fromDesignationId;

    @Column(name = "posting_type", nullable = false, length = 50, updatable = false)
    private String postingType;

    @Column(name = "posting_start_date", nullable = false, updatable = false)
    private LocalDate postingStartDate;

    @Column(name = "posting_end_date")
    private LocalDate postingEndDate;

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

    @Column(name = "office_name", length = 250, updatable = false)
    private String officeName;

    @Column(name = "from_office_name", length = 250, updatable = false)
    private String fromOfficeName;

    @Column(name = "from_location", length = 250, updatable = false)
    private String fromLocation;

    @Column(name = "seat_name", length = 150, updatable = false)
    private String seatName;

    @Column(name = "reporting_officer_id", updatable = false)
    private UUID reportingOfficerId;

    @Column(name = "controlling_officer_id", updatable = false)
    private UUID controllingOfficerId;

    @Column(name = "location", length = 250, updatable = false)
    private String location;

    @Column(name = "district", length = 150, updatable = false)
    private String district;

    @Column(name = "from_district", length = 150, updatable = false)
    private String fromDistrict;

    @Column(name = "transfer_type", length = 50, updatable = false)
    private String transferType;

    @Column(name = "transfer_reason", updatable = false)
    private String transferReason;

    @Column(name = "permanent_charge", nullable = false)
    private Boolean permanentCharge = Boolean.TRUE;

    @Column(name = "additional_charge", nullable = false)
    private Boolean additionalCharge = Boolean.FALSE;

    @Column(name = "current_charge_holder_id", updatable = false)
    private UUID currentChargeHolderId;

    @Column(name = "previous_charge_holder_id", updatable = false)
    private UUID previousChargeHolderId;

    @Column(name = "charge_start_date", updatable = false)
    private LocalDate chargeStartDate;

    @Column(name = "charge_end_date")
    private LocalDate chargeEndDate;

    @Column(name = "charge_status", length = 30)
    private String chargeStatus;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "joining_time")
    private LocalTime joiningTime;

    @Column(name = "joining_order", length = 100)
    private String joiningOrder;

    @Column(name = "joining_remarks")
    private String joiningRemarks;

    @Column(name = "relieving_date")
    private LocalDate relievingDate;

    @Column(name = "relieving_time")
    private LocalTime relievingTime;

    @Column(name = "relieving_order", length = 100)
    private String relievingOrder;

    @Column(name = "relieving_remarks")
    private String relievingRemarks;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "source_entity_type", length = 50, updatable = false)
    private String sourceEntityType;

    @Column(name = "source_entity_id", updatable = false)
    private UUID sourceEntityId;
}
