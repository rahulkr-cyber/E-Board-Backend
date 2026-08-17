package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record StorageConfigurationUpdateRequest(
        @NotBlank(message = "Storage provider is required")
        @Size(max = 50, message = "Storage provider must not exceed 50 characters")
        String providerCode,

        @NotBlank(message = "Storage display name is required")
        @Size(max = 150, message = "Storage display name must not exceed 150 characters")
        String displayName,

        @Size(max = 1000, message = "Storage base path must not exceed 1000 characters")
        String basePath,

        Boolean healthCheckEnabled,

        Map<String, Object> configuration) {
}
