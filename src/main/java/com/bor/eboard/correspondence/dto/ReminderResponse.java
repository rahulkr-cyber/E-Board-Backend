package com.bor.eboard.correspondence.dto;

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
public class ReminderResponse {

    private UUID id;
    private UUID fileId;
    private UUID letterId;
    private String reminderNumber;
    private LocalDate reminderDate;
    private String reminderType;
    private String remarks;
    private UUID generatedBy;
    private String generatedByName;
    private String status;
    private LocalDateTime createdAt;
}
