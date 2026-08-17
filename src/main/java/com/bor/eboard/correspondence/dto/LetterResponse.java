package com.bor.eboard.correspondence.dto;

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
public class LetterResponse {

    private UUID id;
    private UUID fileId;
    private String fileNumber;
    private UUID diaryEntryId;
    private String letterDirection;
    private String letterType;
    private String letterNumber;
    private String referenceNumber;
    private LocalDate letterDate;
    private String subject;
    private String body;
    private String senderName;
    private String senderDesignation;
    private String senderDepartment;
    private String senderAddress;
    private UUID receiverDepartmentId;
    private String receiverDepartmentName;
    private UUID receiverSectionId;
    private String receiverSectionName;
    private UUID receiverUserId;
    private String receiverUserName;
    private UUID categoryId;
    private String categoryName;
    private UUID priorityId;
    private String priorityName;
    private UUID languageId;
    private String languageName;
    private Boolean confidential;
    private LocalDate dueDate;
    private LocalDate reminderDate;
    private UUID currentOwnerId;
    private UUID currentSectionId;
    private String currentStatus;
    private LocalDateTime createdAt;
    private List<AttachmentInfo> attachments;
    private ChecklistDtos.Summary checklistSummary;
    private ChecklistDtos.InstanceResponse checklist;
}
