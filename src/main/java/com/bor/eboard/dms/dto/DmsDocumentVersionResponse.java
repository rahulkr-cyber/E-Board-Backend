package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmsDocumentVersionResponse(
        UUID id,
        Integer versionNumber,
        String originalFileName,
        String mimeType,
        Long fileSize,
        String checksumSha256,
        String versionComment,
        UUID uploadedBy,
        String uploadedByName,
        LocalDateTime uploadedAt) {
}
