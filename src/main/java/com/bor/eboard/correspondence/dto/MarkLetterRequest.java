package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** BCR-03 Part 16: letters are marked exactly like files. */
@Data
public class MarkLetterRequest {

    @NotNull(message = "toUserId is required")
    private UUID toUserId;

    private UUID toRoleId;
    private UUID toSectionId;
    private String remarks;
    private Boolean checklistOverride;
    private String checklistOverrideRemarks;
}
