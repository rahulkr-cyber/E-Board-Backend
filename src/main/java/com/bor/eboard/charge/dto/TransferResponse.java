package com.bor.eboard.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private UUID fromDepartmentId;
    private String fromDepartmentName;
    private UUID fromSectionId;
    private String fromSectionName;
    private UUID toDepartmentId;
    private String toDepartmentName;
    private UUID toSectionId;
    private String toSectionName;
    private UUID fromDesignationId;
    private String fromDesignationName;
    private UUID toDesignationId;
    private String toDesignationName;
    private String postingType;
    private String transferType;
    private String fromOfficeName;
    private String toOfficeName;
    private String seatName;
    private UUID reportingOfficerId;
    private String reportingOfficerName;
    private UUID controllingOfficerId;
    private String controllingOfficerName;
    private String fromLocation;
    private String toLocation;
    private String fromDistrict;
    private String toDistrict;
    private LocalDate transferDate;
    private LocalDate effectiveToDate;
    private String orderNumber;
    private LocalDate orderDate;
    private String governmentOrderNumber;
    private LocalDate governmentOrderDate;
    private String transferReason;
    private String remarks;

    /** The Government Order / Office Order document, if one was uploaded. */
    private UUID attachmentId;
    private String orderDocumentName;
    private Long orderDocumentSize;
    private String orderDocumentMimeType;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
