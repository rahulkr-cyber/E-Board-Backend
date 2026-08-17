package com.bor.eboard.workflow.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.workflow.dto.ApprovalRequest;
import com.bor.eboard.workflow.dto.ForwardRequest;
import com.bor.eboard.workflow.dto.MovementResponse;
import com.bor.eboard.workflow.dto.ReassignRequest;
import com.bor.eboard.workflow.dto.RejectRequest;
import com.bor.eboard.workflow.dto.ReturnRequest;
import com.bor.eboard.workflow.dto.StartWorkflowRequest;
import com.bor.eboard.workflow.dto.WorkflowInstanceResponse;
import com.bor.eboard.workflow.dto.WorkflowTaskResponse;

import java.util.List;
import java.util.UUID;

/**
 * Core workflow engine (08_WORKFLOW_ENGINE.md section 19).
 */
public interface WorkflowService {

    WorkflowInstanceResponse startWorkflow(StartWorkflowRequest request);

    WorkflowInstanceResponse forwardFile(UUID fileId, ForwardRequest request);

    WorkflowInstanceResponse forwardLetter(UUID letterId, ForwardRequest request);

    WorkflowInstanceResponse approveFile(UUID fileId, ApprovalRequest request);

    WorkflowInstanceResponse approveLetter(UUID letterId, ApprovalRequest request);

    WorkflowInstanceResponse returnFile(UUID fileId, ReturnRequest request);

    WorkflowInstanceResponse returnLetter(UUID letterId, ReturnRequest request);

    WorkflowInstanceResponse rejectFile(UUID fileId, RejectRequest request);

    WorkflowInstanceResponse rejectLetter(UUID letterId, RejectRequest request);

    WorkflowInstanceResponse reassignFile(UUID fileId, ReassignRequest request);

    WorkflowInstanceResponse reassignLetter(UUID letterId, ReassignRequest request);

    PageResponse<WorkflowTaskResponse> getMyPendingTasks(boolean overdueOnly,
                                                         PaginationRequest pagination);

    List<MovementResponse> getFileHistory(UUID fileId);

    List<MovementResponse> getLetterHistory(UUID letterId);
}
