package com.bor.eboard.registry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Row of the Diary Register report (01_PROJECT.md section 14):
 * chronological record of all correspondence registered via Registry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryRegisterRow {

    private UUID id;
    private String diaryNumber;
    private LocalDateTime receivedDate;
    private String senderName;
    private String senderDepartment;
    private String originalLetterNumber;
    private String subject;
    private String categoryName;
    private String priorityName;
    private String initialSectionName;
    private String status;
}
