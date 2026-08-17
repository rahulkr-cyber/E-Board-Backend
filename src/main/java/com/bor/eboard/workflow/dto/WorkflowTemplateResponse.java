package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID departmentId;
    private String departmentName;
    private UUID sectionId;
    private String sectionName;
    private UUID priorityId;
    private String priorityName;
    private Boolean active;
    private LocalDateTime createdAt;
    private List<WorkflowStepResponse> steps;
}
