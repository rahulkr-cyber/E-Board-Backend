package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DmsDocumentSearchRequest(
        @Size(max = 250) String keywords,
        @Size(max = 50) String documentNumber,
        UUID documentTypeId,
        @Size(max = 20) List<@Size(max = 100) String> tags,
        LocalDateTime uploadedFrom,
        LocalDateTime uploadedTo,
        LocalDateTime modifiedFrom,
        LocalDateTime modifiedTo,
        UUID uploadedBy,
        UUID departmentId,
        UUID sectionId,
        @Size(max = 30) String status,
        @Min(0) Long minFileSizeBytes,
        @Min(0) Long maxFileSizeBytes,
        @Size(max = 30) Map<@Size(max = 80) String, Object> metadata,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        @Size(max = 30) String sortBy,
        @Size(max = 4) String sortDirection) {
}
