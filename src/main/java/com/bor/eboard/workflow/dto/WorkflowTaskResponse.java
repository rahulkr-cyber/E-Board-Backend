package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskResponse {

    private UUID id;
    private UUID workflowInstanceId;
    private UUID fileId;
    private UUID letterId;
    private String fileNumber;
    private String fileSubject;
    private UUID stepId;
    private String stepName;
    private UUID assignedToUserId;
    private String assignedToUserName;
    private UUID assignedToRoleId;
    private String assignedToRoleName;
    private UUID assignedToSectionId;
    private String assignedToSectionName;
    private String status;
    private LocalDate dueDate;
    private boolean overdue;
    private String priorityName;
    private LocalDateTime assignedAt;
}
