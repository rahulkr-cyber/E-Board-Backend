package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;

public record StorageHealthResponse(
        String providerCode,
        boolean healthy,
        String message,
        LocalDateTime checkedAt) {
}
