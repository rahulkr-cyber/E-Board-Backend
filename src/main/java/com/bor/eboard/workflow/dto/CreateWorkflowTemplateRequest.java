package com.bor.eboard.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Create workflow template (04_API_SPEC.md 8.1). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowTemplateRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private UUID categoryId;
    private UUID departmentId;
    private UUID sectionId;
    private UUID priorityId;
}
