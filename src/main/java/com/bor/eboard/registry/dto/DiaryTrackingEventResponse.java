package com.bor.eboard.registry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTrackingEventResponse {
    private LocalDateTime timestamp;
    private String eventType;
    private String action;
    private String title;
    private String detail;
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private UUID fromSectionId;
    private String fromSectionName;
    private UUID toSectionId;
    private String toSectionName;
    private UUID referenceId;
}
