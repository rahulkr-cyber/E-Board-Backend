package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Create a reminder linked to a file/letter (03_DATABASE.md 8.5).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReminderRequest {

    private UUID letterId;

    @NotNull(message = "Reminder date is required")
    private LocalDate reminderDate;

    @Size(max = 50)
    private String reminderType;

    private String remarks;
}
