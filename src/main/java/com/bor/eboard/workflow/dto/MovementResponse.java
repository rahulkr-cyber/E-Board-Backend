package com.bor.eboard.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One immutable movement in a file's history (04_API_SPEC.md 8.10). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResponse {

    private UUID id;
    private UUID fileId;
    private UUID workflowInstanceId;
    private String action;
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private UUID fromSectionId;
    private String fromSectionName;
    private UUID toSectionId;
    private String toSectionName;
    private String remarks;
    private LocalDateTime actionAt;
    private String actorName;
}
