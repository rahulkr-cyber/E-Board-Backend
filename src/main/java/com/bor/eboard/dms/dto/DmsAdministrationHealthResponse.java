package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DmsAdministrationHealthResponse(
        boolean healthy,
        boolean moduleEnabled,
        StorageHealthResponse storage,
        List<DmsConfigurationCheckResponse> checks,
        LocalDateTime checkedAt) {
}
