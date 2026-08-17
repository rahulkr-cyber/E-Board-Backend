package com.bor.eboard.workflow.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.workflow.dto.AddWorkflowStepRequest;
import com.bor.eboard.workflow.dto.ApprovalRequest;
import com.bor.eboard.workflow.dto.CreateWorkflowTemplateRequest;
import com.bor.eboard.workflow.dto.ForwardRequest;
import com.bor.eboard.workflow.dto.MovementResponse;
import com.bor.eboard.workflow.dto.ReassignRequest;
import com.bor.eboard.workflow.dto.RejectRequest;
import com.bor.eboard.workflow.dto.ReturnRequest;
import com.bor.eboard.workflow.dto.StartWorkflowRequest;
import com.bor.eboard.workflow.dto.WorkflowInstanceResponse;
import com.bor.eboard.workflow.dto.WorkflowStepResponse;
import com.bor.eboard.workflow.dto.WorkflowTaskResponse;
import com.bor.eboard.workflow.dto.WorkflowTemplateResponse;
import com.bor.eboard.workflow.service.WorkflowService;
import com.bor.eboard.workflow.service.WorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Workflow engine APIs (04_API_SPEC.md section 8).
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "DB-driven workflow templates, tasks and file movement")
public class WorkflowController {

    private final WorkflowTemplateService templateService;
    private final WorkflowService workflowService;

    // ---- Template configuration ----

    @Operation(summary = "Create a workflow template")
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('WORKFLOW_CONFIGURE')")
    public ApiResponse<WorkflowTemplateResponse> createTemplate(
            @Valid @RequestBody CreateWorkflowTemplateRequest request) {
        return ApiResponse.success("Workflow template created successfully",
                templateService.createTemplate(request));
    }

    @Operation(summary = "Add a step to a workflow template")
    @PostMapping("/templates/{id}/steps")
    @PreAuthorize("hasAuthority('WORKFLOW_CONFIGURE')")
    public ApiResponse<WorkflowStepResponse> addStep(
            @PathVariable UUID id, @Valid @RequestBody AddWorkflowStepRequest request) {
        return ApiResponse.success("Workflow step added successfully",
                templateService.addStep(id, request));
    }

    @Operation(summary = "Get a workflow template with its steps")
    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ApiResponse<WorkflowTemplateResponse> getTemplate(@PathVariable UUID id) {
        return ApiResponse.success(templateService.getTemplate(id));
    }

    @Operation(summary = "List workflow templates")
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ApiResponse<List<WorkflowTemplateResponse>> listTemplates() {
        return ApiResponse.success(templateService.listTemplates());
    }

    @Operation(summary = "List steps of a workflow template")
    @GetMapping("/templates/{id}/steps")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ApiResponse<List<WorkflowStepResponse>> listSteps(@PathVariable UUID id) {
        return ApiResponse.success(templateService.listSteps(id));
    }

    // ---- Workflow actions ----

    @Operation(summary = "Start a workflow on a file")
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('WORKFLOW_START')")
    public ApiResponse<WorkflowInstanceResponse> start(
            @Valid @RequestBody StartWorkflowRequest request) {
        return ApiResponse.success("Workflow started successfully",
                workflowService.startWorkflow(request));
    }

    @Operation(summary = "Forward a file to the next step")
    @PostMapping("/files/{fileId}/forward")
    @PreAuthorize("hasAuthority('WORKFLOW_FORWARD')")
    public ApiResponse<WorkflowInstanceResponse> forward(
            @PathVariable UUID fileId, @RequestBody ForwardRequest request) {
        return ApiResponse.success("File forwarded successfully",
                workflowService.forwardFile(fileId, request));
    }


    @Operation(summary = "Forward a letter to the next step")
    @PostMapping("/letters/{letterId}/forward")
    @PreAuthorize("hasAuthority('WORKFLOW_FORWARD')")
    public ApiResponse<WorkflowInstanceResponse> forwardLetter(
            @PathVariable UUID letterId, @RequestBody ForwardRequest request) {
        return ApiResponse.success("Letter forwarded successfully",
                workflowService.forwardLetter(letterId, request));
    }

    @Operation(summary = "Approve the current step")
    @PostMapping("/files/{fileId}/approve")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE')")
    public ApiResponse<WorkflowInstanceResponse> approve(
            @PathVariable UUID fileId, @RequestBody ApprovalRequest request) {
        return ApiResponse.success("File approved successfully",
                workflowService.approveFile(fileId, request));
    }


    @Operation(summary = "Approve the current Letter step")
    @PostMapping("/letters/{letterId}/approve")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE')")
    public ApiResponse<WorkflowInstanceResponse> approveLetter(
            @PathVariable UUID letterId, @RequestBody ApprovalRequest request) {
        return ApiResponse.success("Letter approved successfully",
                workflowService.approveLetter(letterId, request));
    }

    @Operation(summary = "Return a file (remarks mandatory)")
    @PostMapping("/files/{fileId}/return")
    @PreAuthorize("hasAuthority('WORKFLOW_RETURN')")
    public ApiResponse<WorkflowInstanceResponse> returnFile(
            @PathVariable UUID fileId, @Valid @RequestBody ReturnRequest request) {
        return ApiResponse.success("File returned successfully",
                workflowService.returnFile(fileId, request));
    }


    @Operation(summary = "Return a letter (remarks mandatory)")
    @PostMapping("/letters/{letterId}/return")
    @PreAuthorize("hasAuthority('WORKFLOW_RETURN')")
    public ApiResponse<WorkflowInstanceResponse> returnLetter(
            @PathVariable UUID letterId, @Valid @RequestBody ReturnRequest request) {
        return ApiResponse.success("Letter returned successfully",
                workflowService.returnLetter(letterId, request));
    }

    @Operation(summary = "Reject a file (remarks mandatory)")
    @PostMapping("/files/{fileId}/reject")
    @PreAuthorize("hasAuthority('WORKFLOW_REJECT')")
    public ApiResponse<WorkflowInstanceResponse> reject(
            @PathVariable UUID fileId, @Valid @RequestBody RejectRequest request) {
        return ApiResponse.success("File rejected successfully",
                workflowService.rejectFile(fileId, request));
    }


    @Operation(summary = "Reject a letter (remarks mandatory)")
    @PostMapping("/letters/{letterId}/reject")
    @PreAuthorize("hasAuthority('WORKFLOW_REJECT')")
    public ApiResponse<WorkflowInstanceResponse> rejectLetter(
            @PathVariable UUID letterId, @Valid @RequestBody RejectRequest request) {
        return ApiResponse.success("Letter rejected successfully",
                workflowService.rejectLetter(letterId, request));
    }

    @Operation(summary = "Reassign the current task to another user")
    @PostMapping("/files/{fileId}/reassign")
    @PreAuthorize("hasAuthority('WORKFLOW_REASSIGN')")
    public ApiResponse<WorkflowInstanceResponse> reassign(
            @PathVariable UUID fileId, @Valid @RequestBody ReassignRequest request) {
        return ApiResponse.success("File reassigned successfully",
                workflowService.reassignFile(fileId, request));
    }


    @Operation(summary = "Reassign the current Letter task to another user")
    @PostMapping("/letters/{letterId}/reassign")
    @PreAuthorize("hasAuthority('WORKFLOW_REASSIGN')")
    public ApiResponse<WorkflowInstanceResponse> reassignLetter(
            @PathVariable UUID letterId, @Valid @RequestBody ReassignRequest request) {
        return ApiResponse.success("Letter reassigned successfully",
                workflowService.reassignLetter(letterId, request));
    }

    // ---- Tasks & history ----

    @Operation(summary = "My pending workflow tasks")
    @GetMapping("/tasks/my")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ApiResponse<PageResponse<WorkflowTaskResponse>> myTasks(
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationRequest pagination = new PaginationRequest(page, size, sortBy, sortDir);
        return ApiResponse.success(workflowService.getMyPendingTasks(overdueOnly, pagination));
    }

    @Operation(summary = "File movement history")
    @GetMapping("/files/{fileId}/history")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ApiResponse<List<MovementResponse>> history(@PathVariable UUID fileId) {
        return ApiResponse.success(workflowService.getFileHistory(fileId));
    }
    @Operation(summary = "Letter movement history")
    @GetMapping("/letters/{letterId}/history")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ApiResponse<List<MovementResponse>> letterHistory(@PathVariable UUID letterId) {
        return ApiResponse.success(workflowService.getLetterHistory(letterId));
    }

}
