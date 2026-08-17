package com.bor.eboard.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignmentRuleRequest {

    @NotNull(message = "fromRoleId is required")
    private UUID fromRoleId;

    @NotNull(message = "toRoleId is required")
    private UUID toRoleId;

    /** NULL = rule applies to any section. */
    private UUID allowedSectionId;

    private Boolean sameSectionAllowed = Boolean.TRUE;
    private Boolean crossSectionAllowed = Boolean.FALSE;
    private Boolean multiUserAllowed = Boolean.FALSE;
    private Boolean active = Boolean.TRUE;
    private String remarks;
}
