package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmsDocumentTypeResponse(
        UUID id,
        String code,
        String name,
        String description,
        Integer sortOrder,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
