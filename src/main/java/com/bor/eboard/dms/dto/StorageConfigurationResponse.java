package com.bor.eboard.dms.dto;

import java.util.UUID;

public record StorageConfigurationResponse(
        UUID id,
        String providerCode,
        String displayName,
        String effectiveBasePath,
        boolean active,
        boolean primaryProvider,
        boolean healthCheckEnabled,
        boolean persisted) {
}
