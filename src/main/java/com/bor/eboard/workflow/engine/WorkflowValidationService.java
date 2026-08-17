package com.bor.eboard.workflow.engine;

import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.WorkflowException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.workflow.entity.WorkflowStep;
import com.bor.eboard.workflow.entity.WorkflowTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Centralised pre-action validation (08_WORKFLOW_ENGINE.md section 18):
 * user authenticated + active, has permission, file not closed/archived,
 * task pending, user assigned (or charge holder — Phase 9), action allowed
 * for the current step.
 */
@Service
@RequiredArgsConstructor
public class WorkflowValidationService {

    private final IdentityFacade identityFacade;
    private final CorrespondenceFacade correspondenceFacade;

    /** Resolve and return the current authenticated, active user id. */
    public UUID requireActiveUser() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        IdentityFacade.UserRef user = identityFacade.findUser(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!user.active()) {
            throw new ForbiddenException("User account is not active");
        }
        return userId;
    }

    public void requirePermission(String permission) {
        if (!SecurityUtils.hasAuthority(permission)) {
            throw new ForbiddenException("Missing permission: " + permission);
        }
    }

    public void requireFileOpen(UUID fileId) {
        CorrespondenceFacade.FileState state = correspondenceFacade.findFileState(fileId)
                .orElseThrow(() -> new WorkflowException("File not found"));
        if (state.closed()) {
            throw new WorkflowException(
                    "File is " + state.currentStatus() + " and cannot move in a workflow");
        }
    }

    public void requireLetterOpen(UUID letterId) {
        CorrespondenceFacade.LetterState state = correspondenceFacade.findLetterState(letterId)
                .orElseThrow(() -> new WorkflowException("Letter not found"));
        if (state.closed()) {
            throw new WorkflowException(
                    "Letter is " + state.currentStatus() + " and cannot move in a workflow");
        }
    }

    public void requireTaskPending(WorkflowTask task) {
        if (task == null) {
            throw new WorkflowException("No pending workflow task");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new WorkflowException("Task is not pending (status " + task.getStatus() + ")");
        }
    }

    /** The acting user must satisfy the task's user/role/section assignment. */
    public void requireAssignedUser(UUID userId, WorkflowTask task) {
        boolean matches = identityFacade.userMatchesAssignment(
                userId, task.getAssignedToUserId(),
                task.getAssignedToRoleId(), task.getAssignedToSectionId());
        if (!matches) {
            throw new ForbiddenException("This task is not assigned to you");
        }
    }

    public void requireReturnAllowed(WorkflowStep step) {
        if (Boolean.FALSE.equals(step.getCanReturn())) {
            throw new WorkflowException("Return is not permitted at this step");
        }
    }

    public void requireRejectAllowed(WorkflowStep step) {
        if (Boolean.FALSE.equals(step.getCanReject())) {
            throw new WorkflowException("Reject is not permitted at this step");
        }
    }

    public void requireReassignAllowed(WorkflowStep step) {
        if (Boolean.FALSE.equals(step.getCanReassign())) {
            throw new WorkflowException("Reassign is not permitted at this step");
        }
    }
}
