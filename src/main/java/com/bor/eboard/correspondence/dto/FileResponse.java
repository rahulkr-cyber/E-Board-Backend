package com.bor.eboard.correspondence.dto;

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
public class FileResponse {

    private UUID id;
    private String fileNumber;
    private Integer fileYear;
    private Long fileSequence;
    private String subject;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID priorityId;
    private String priorityName;
    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private UUID currentOwnerId;
    private String currentOwnerName;
    private UUID currentSectionId;
    private String currentSectionName;
    private String currentStatus;
    private LocalDate openedDate;
    private LocalDate closedDate;
    private LocalDate archivedDate;
    private Boolean confidential;
    private LocalDateTime createdAt;
    private List<LetterResponse> letters;
    private List<AttachmentInfo> attachments;
}
