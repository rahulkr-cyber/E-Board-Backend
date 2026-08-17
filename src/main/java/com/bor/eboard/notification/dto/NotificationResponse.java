package com.bor.eboard.notification.dto;

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
public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private String notificationType;
    private String priority;
    private String linkedEntityType;
    private UUID linkedEntityId;
    private Boolean readFlag;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
