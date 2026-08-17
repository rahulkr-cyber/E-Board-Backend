package com.bor.eboard.correspondence.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReopenHistoryResponse {
    private UUID id;
    private UUID fileId;
    private String reopenedByName;
    private LocalDateTime reopenedAt;
    private String previousStatus;
    private String reason;
}
