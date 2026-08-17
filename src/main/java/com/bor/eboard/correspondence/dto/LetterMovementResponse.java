package com.bor.eboard.correspondence.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LetterMovementResponse {
    private UUID id;
    private String action;
    private String fromUserName;
    private String toUserName;
    private String fromSectionName;
    private String toSectionName;
    private String remarks;
    private LocalDateTime actionAt;
}
