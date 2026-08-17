package com.bor.eboard.correspondence.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FollowupResponse {
    private UUID id;
    private UUID dispatchId;
    private String followupNumber;
    private LocalDate followupDate;
    private LocalDate dueDate;
    private LocalDate reminderDate;
    private Boolean replyReceived;
    private LocalDate replyReceivedDate;
    private String status;
    private String remarks;

    /** Days elapsed past the due date; 0 when not overdue. */
    private Long pendingDays;
    private UUID fileId;
    private UUID letterId;
    private String followupType;
    private LocalDate nextFollowupDate;
    private LocalDateTime createdAt;
}
