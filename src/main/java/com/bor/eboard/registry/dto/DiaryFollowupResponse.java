package com.bor.eboard.registry.dto;

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
public class DiaryFollowupResponse {
    private UUID id;
    private LocalDate followupDate;
    private String followupType;
    private LocalDate dueDate;
    private LocalDate reminderDate;
    private LocalDate nextFollowupDate;
    private Boolean replyReceived;
    private LocalDate replyReceivedDate;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
}
