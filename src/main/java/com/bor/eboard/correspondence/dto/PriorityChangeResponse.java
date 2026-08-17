package com.bor.eboard.correspondence.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PriorityChangeResponse {
    private UUID id;
    private UUID fileId;
    private String oldPriorityName;
    private String newPriorityName;
    private String changedByName;
    private LocalDateTime changedAt;
    private String remarks;
}
