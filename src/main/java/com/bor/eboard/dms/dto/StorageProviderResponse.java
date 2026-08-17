package com.bor.eboard.dms.dto;

public record StorageProviderResponse(
        String providerCode,
        boolean configured,
        boolean active,
        boolean primaryProvider,
        boolean healthy,
        String message) {
}
