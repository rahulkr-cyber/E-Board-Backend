package com.bor.eboard.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Reassign file (04_API_SPEC.md 8.8). Same step, new owner. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReassignRequest {

    @NotNull(message = "Target user is required for reassignment")
    private UUID toUserId;

    private String remarks;
}
