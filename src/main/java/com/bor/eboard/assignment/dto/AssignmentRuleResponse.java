package com.bor.eboard.assignment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AssignmentRuleResponse {
    private UUID id;
    private UUID fromRoleId;
    private String fromRoleName;
    private UUID toRoleId;
    private String toRoleName;
    private UUID allowedSectionId;
    private String allowedSectionName;
    private Boolean sameSectionAllowed;
    private Boolean crossSectionAllowed;
    private Boolean multiUserAllowed;
    private Boolean active;
    private String remarks;
}
