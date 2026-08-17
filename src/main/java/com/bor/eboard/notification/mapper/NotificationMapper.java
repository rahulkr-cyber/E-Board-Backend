package com.bor.eboard.notification.mapper;

import com.bor.eboard.notification.dto.NotificationResponse;
import com.bor.eboard.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .priority(n.getPriority())
                .linkedEntityType(n.getLinkedEntityType())
                .linkedEntityId(n.getLinkedEntityId())
                .readFlag(n.getReadFlag())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
