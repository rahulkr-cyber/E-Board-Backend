package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Approve file (04_API_SPEC.md 8.5). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    private String remarks;
}
