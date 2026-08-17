package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateFollowupRequest {

    private UUID letterId;

    private String followupType;

    private String remarks;

    // Existing fields (if you still need them)
    private LocalDate followupDate;
    private LocalDate nextFollowupDate;

    // Add these because the service expects them
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private LocalDate reminderDate;
}