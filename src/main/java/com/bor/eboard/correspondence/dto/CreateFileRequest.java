package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Create file (04_API_SPEC.md 7.1). A file may be opened from a diary
 * entry (diaryEntryId links the originating inward letter) or standalone.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFileRequest {

    /** Optional: diary entry this file is opened from. */
    private UUID diaryEntryId;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Priority is required")
    private UUID priorityId;

    @NotNull(message = "Department is required")
    private UUID departmentId;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    private Boolean confidential;
}
