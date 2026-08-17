package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Start workflow (04_API_SPEC.md 8.3). Template optional: auto-resolved if null. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkflowRequest {

    private UUID fileId;

    private UUID letterId;

    private UUID workflowTemplateId;

    private String remarks;

    /** Backward-compatible constructor for existing File workflow callers. */
    public StartWorkflowRequest(UUID fileId, UUID workflowTemplateId, String remarks) {
        this.fileId = fileId;
        this.workflowTemplateId = workflowTemplateId;
        this.remarks = remarks;
    }
}
