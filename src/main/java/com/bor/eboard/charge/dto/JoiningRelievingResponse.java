package com.bor.eboard.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoiningRelievingResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private String eventType;
    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private UUID designationId;
    private String designationName;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String orderNumber;
    private LocalDate orderDate;
    private String governmentOrderNumber;
    private LocalDate governmentOrderDate;
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
