package com.bor.eboard.registry.dto;

import com.bor.eboard.checklist.dto.ChecklistDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntryResponse {

    private UUID id;
    private String diaryNumber;
    private Integer diaryYear;
    private Long diarySequence;
    private String sourceType;
    private String receivedMode;
    private LocalDateTime receivedDate;
    private UUID receivedBy;
    private String receivedByName;
    private String senderName;
    private String senderDesignation;
    private String senderDepartment;
    private String senderAddress;
    private String senderEmail;
    private String senderMobile;
    private String originalLetterNumber;
    private String referenceNumber;
    private LocalDate letterDate;
    private String subject;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID priorityId;
    private String priorityName;
    private UUID languageId;
    private String languageName;
    private Boolean confidential;
    private LocalDate dueDate;
    private LocalDate reminderDate;
    private Integer pageCount;
    private Boolean physicalCopyReceived;
    private String barcodeValue;
    private String qrCodeValue;
    private UUID initialDepartmentId;
    private String initialDepartmentName;
    private UUID initialSectionId;
    private String initialSectionName;
    private UUID initialAssignedUserId;
    private String initialAssignedUserName;
    private String status;
    private LocalDateTime createdAt;
    private ReceiptResponse receipt;
    private List<AttachmentResponse> attachments;

    // Live read-only state from the linked Letter after forwarding.
    private UUID linkedLetterId;
    private UUID currentOwnerId;
    private String currentOwnerName;
    private UUID currentSectionId;
    private String currentSectionName;
    private String linkedLetterStatus;
    private LocalDate linkedLetterDueDate;
    private String finalDisposalStatus;
    private LocalDateTime disposedAt;
    private List<AttachmentResponse> workflowAttachments;
    private UUID workflowInstanceId;
    private String workflowStatus;
    private UUID workflowStageId;
    private Integer workflowStageOrder;
    private String workflowStage;
    private LocalDateTime pendingSince;
    private Long remainingDays;
    private Long overdueDays;
    private String slaStatus;
    private String escalationStatus;
    private List<DiaryTrackingEventResponse> movementHistory;
    private List<DiaryTrackingEventResponse> timeline;
    private List<DiaryFollowupResponse> followups;
    private ChecklistDtos.Summary checklistSummary;
    private ChecklistDtos.InstanceResponse checklist;
    
}
