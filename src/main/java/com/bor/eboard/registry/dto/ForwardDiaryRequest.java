package com.bor.eboard.registry.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Forward diary to section (04_API_SPEC.md 6.3).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForwardDiaryRequest {

    @NotNull(message = "Target section is required")
    private UUID sectionId;

    /** Optional: specific officer within the section. */
    private UUID userId;

    private String remarks;

    /** Explicit, permission-controlled override when the mapped checklist is incomplete. */
    private Boolean checklistOverride;
    private String checklistOverrideRemarks;
}
