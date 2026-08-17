package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmsAuditEventResponse(
        UUID id,
        UUID documentId,
        String entityType,
        UUID entityId,
        String action,
        String oldValue,
        String newValue,
        UUID actorId,
        String actorName,
        String ipAddress,
        String apiPath,
        String httpMethod,
        boolean success,
        String errorMessage,
        LocalDateTime createdAt) {
}
