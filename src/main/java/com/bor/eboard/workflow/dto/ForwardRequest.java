package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Forward file (04_API_SPEC.md 8.4). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForwardRequest {

    private UUID toUserId;
    private UUID toSectionId;
    private String remarks;
    private Boolean checklistOverride;
    private String checklistOverrideRemarks;
}
