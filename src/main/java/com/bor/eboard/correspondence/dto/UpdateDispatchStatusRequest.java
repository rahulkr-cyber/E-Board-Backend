package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update dispatch status.
 * Statuses: PENDING | DISPATCHED | DELIVERED | RETURNED | CANCELLED.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDispatchStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|DISPATCHED|DELIVERED|RETURNED|CANCELLED",
            message = "Status must be one of PENDING, DISPATCHED, DELIVERED, RETURNED, CANCELLED")
    private String status;

    private String trackingNumber;

    private String remarks;
}
