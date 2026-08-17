package com.bor.eboard.dms.audit;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmsAuditTrailEvent(
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
        String historySummary,
        Integer versionNumber,
        String snapshotJson,
        LocalDateTime createdAt) {
}
