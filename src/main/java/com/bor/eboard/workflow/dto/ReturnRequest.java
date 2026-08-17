package com.bor.eboard.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Return file (04_API_SPEC.md 8.6). Remarks mandatory (section 9). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    private UUID returnToUserId;
    private UUID returnToSectionId;

    @NotBlank(message = "Remarks are mandatory when returning a file")
    private String remarks;
}
