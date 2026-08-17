package com.bor.eboard.workflow.listener;

import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.workflow.dto.StartWorkflowRequest;
import com.bor.eboard.workflow.event.LetterCreatedEvent;
import com.bor.eboard.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class WorkflowStartListener {

    private final WorkflowService workflowService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LetterCreatedEvent event) {

        StartWorkflowRequest req = new StartWorkflowRequest(
            null,
            event.letterId(),
            null,
            event.remarks()
        );

        workflowService.startWorkflow(req);
    }
}