package com.bor.eboard.workflow.service;

import com.bor.eboard.workflow.dto.AddWorkflowStepRequest;
import com.bor.eboard.workflow.dto.CreateWorkflowTemplateRequest;
import com.bor.eboard.workflow.dto.WorkflowStepResponse;
import com.bor.eboard.workflow.dto.WorkflowTemplateResponse;

import java.util.List;
import java.util.UUID;

/** Template/step configuration (04_API_SPEC.md 8.1-8.2). */
public interface WorkflowTemplateService {

    WorkflowTemplateResponse createTemplate(CreateWorkflowTemplateRequest request);

    WorkflowStepResponse addStep(UUID templateId, AddWorkflowStepRequest request);

    WorkflowTemplateResponse getTemplate(UUID templateId);

    List<WorkflowTemplateResponse> listTemplates();

    List<WorkflowStepResponse> listSteps(UUID templateId);
}
