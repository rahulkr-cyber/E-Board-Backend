package com.bor.eboard.workflow.engine;

import com.bor.eboard.workflow.entity.WorkflowMovement;
import com.bor.eboard.workflow.repository.WorkflowMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Records immutable workflow movements. Every workflow action inserts one
 * (08_WORKFLOW_ENGINE.md section 15). IP address and machine name are
 * captured from the current request when available. Runs MANDATORY so a
 * movement can only be written inside an action's transaction.
 */
@Service
@RequiredArgsConstructor
public class WorkflowMovementService {

    private final WorkflowMovementRepository movementRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public WorkflowMovement record(UUID fileId, UUID letterId, UUID workflowInstanceId,
                                   UUID fromUserId, UUID toUserId,
                                   UUID fromSectionId, UUID toSectionId,
                                   String action, String remarks) {
        WorkflowMovement movement = new WorkflowMovement();
        movement.setFileId(fileId);
        movement.setLetterId(letterId);
        movement.setWorkflowInstanceId(workflowInstanceId);
        movement.setFromUserId(fromUserId);
        movement.setToUserId(toUserId);
        movement.setFromSectionId(fromSectionId);
        movement.setToSectionId(toSectionId);
        movement.setAction(action);
        movement.setRemarks(remarks);
        movement.setIpAddress(currentIp());
        movement.setMachineName(currentMachine());
        return movementRepository.save(movement);
    }

    /** Immutable movement history for a file, oldest first. */
    @Transactional(readOnly = true)
    public java.util.List<WorkflowMovement> history(UUID fileId) {
        return movementRepository.findByFileIdOrderByActionAtAsc(fileId);
    }

    /** Immutable movement history for a letter, oldest first. */
    @Transactional(readOnly = true)
    public java.util.List<WorkflowMovement> letterHistory(UUID letterId) {
        return movementRepository.findByLetterIdOrderByActionAtAsc(letterId);
    }

    private String currentIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String currentMachine() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getRemoteHost() : null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
