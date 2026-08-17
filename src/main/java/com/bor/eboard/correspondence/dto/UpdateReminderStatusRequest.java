package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update reminder status.
 * Statuses: DRAFT | SENT | ACKNOWLEDGED | CLOSED.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReminderStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "DRAFT|SENT|ACKNOWLEDGED|CLOSED",
            message = "Status must be one of DRAFT, SENT, ACKNOWLEDGED, CLOSED")
    private String status;

    private String remarks;
}
