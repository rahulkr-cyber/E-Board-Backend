package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DmsDocumentSearchResponse(
        UUID id,
        String documentNumber,
        UUID documentTypeId,
        String documentTypeCode,
        String documentTypeName,
        String title,
        String description,
        String status,
        Integer currentVersionNumber,
        UUID uploadedBy,
        String uploadedByName,
        UUID departmentId,
        String departmentName,
        UUID sectionId,
        String sectionName,
        LocalDateTime uploadedAt,
        LocalDateTime updatedAt,
        Map<String, Object> metadata,
        List<String> tags,
        String latestFileName,
        String latestMimeType,
        Long latestFileSize) {
}
