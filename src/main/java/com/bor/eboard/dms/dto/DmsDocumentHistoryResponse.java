package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record DmsDocumentHistoryResponse(
        UUID id,
        UUID documentId,
        String eventType,
        String entityType,
        UUID entityId,
        Integer versionNumber,
        String summary,
        Map<String, Object> snapshot,
        UUID actorId,
        String actorName,
        LocalDateTime createdAt) {
}
