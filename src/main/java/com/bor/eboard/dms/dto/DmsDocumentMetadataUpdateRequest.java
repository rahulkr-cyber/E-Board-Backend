package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record DmsDocumentMetadataUpdateRequest(
        @NotBlank @Size(max = 250) String title,
        @Size(max = 2000) String description,
        @NotNull Map<String, Object> metadata,
        List<@NotBlank @Size(max = 100) String> tags) {
}
