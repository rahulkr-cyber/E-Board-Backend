package com.bor.eboard.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Add workflow step (04_API_SPEC.md 8.2). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddWorkflowStepRequest {

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    @NotBlank(message = "Step name is required")
    private String stepName;

    private UUID roleId;
    private UUID designationId;
    private UUID sectionId;
    private UUID specificUserId;
    private Boolean approvalRequired;
    private Boolean canReturn;
    private Boolean canReassign;
    private Boolean canReject;
    private Integer slaDays;
    private Boolean parallelStep;
}
