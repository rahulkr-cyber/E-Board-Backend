package com.bor.eboard.register.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** A single line of the Section / Central File Register (BCR-03 Part 11). */
@Data
@Builder
public class RegisterRow {
    private UUID fileId;
    private String fileNumber;
    private String diaryNumber;
    private String subject;
    private String currentHolder;
    private String sectionName;
    private String priorityName;
    private String status;
    private Long pendingDays;
    private LocalDate createdDate;
}
