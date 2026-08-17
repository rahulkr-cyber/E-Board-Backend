package com.bor.eboard.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reject file (04_API_SPEC.md 8.7). Remarks mandatory (section 10). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectRequest {

    @NotBlank(message = "Remarks are mandatory when rejecting a file")
    private String remarks;
}
