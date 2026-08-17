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
public class ChargeResponse {

    private UUID id;
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private String chargeType;
    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String orderNumber;
    private LocalDate orderDate;
    private UUID approvedBy;
    private String approvedByName;
    /** The stored Government Order / Office Order document, if one was uploaded. */
    private UUID attachmentId;
    private String orderDocumentName;
    private Long orderDocumentSize;
    private String orderDocumentMimeType;
    private String reason;
    private String status;
    private boolean currentlyActive;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
