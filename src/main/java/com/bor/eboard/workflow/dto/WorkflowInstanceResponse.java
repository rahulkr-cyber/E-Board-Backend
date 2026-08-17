package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceResponse {

    private UUID id;
    private UUID workflowTemplateId;
    private String workflowTemplateName;
    private UUID fileId;
    private UUID letterId;
    private UUID currentStepId;
    private Integer currentStepOrder;
    private String currentStepName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
