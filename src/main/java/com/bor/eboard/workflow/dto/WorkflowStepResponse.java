package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepResponse {

    private UUID id;
    private UUID workflowTemplateId;
    private Integer stepOrder;
    private String stepName;
    private UUID roleId;
    private String roleName;
    private UUID designationId;
    private UUID sectionId;
    private String sectionName;
    private UUID specificUserId;
    private String specificUserName;
    private Boolean approvalRequired;
    private Boolean canReturn;
    private Boolean canReassign;
    private Boolean canReject;
    private Integer slaDays;
    private Boolean parallelStep;
}
