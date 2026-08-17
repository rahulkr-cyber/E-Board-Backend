package com.bor.eboard.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingTimelineEntryResponse {
    private UUID id;
    private UUID detailedPostingId;
    private UUID sourceRecordId;
    private String sourceType;
    private String postingType;
    private String postingOrderNumber;
    private LocalDate postingOrderDate;
    private String governmentOrderNumber;
    private LocalDate governmentOrderDate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private UUID designationId;
    private String designationName;
    private String officeName;
    private String seatName;
    private String reportingOfficerName;
    private String controllingOfficerName;
    private String location;
    private String district;

    private String fromDepartmentName;
    private String fromSectionName;
    private String fromDesignationName;
    private String fromOfficeName;
    private String fromLocation;
    private String fromDistrict;
    private String transferType;
    private String transferReason;

    private Boolean permanentCharge;
    private Boolean additionalCharge;
    private String currentChargeHolderName;
    private String previousChargeHolderName;
    private LocalDate chargeStartDate;
    private LocalDate chargeEndDate;
    private String chargeStatus;

    private LocalDate joiningDate;
    private LocalTime joiningTime;
    private String joiningOrder;
    private String joiningRemarks;
    private LocalDate relievingDate;
    private LocalTime relievingTime;
    private String relievingOrder;
    private String relievingRemarks;

    private String status;
    private String remarks;
    private UUID primaryAttachmentId;
    private List<PostingDocumentResponse> documents;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
