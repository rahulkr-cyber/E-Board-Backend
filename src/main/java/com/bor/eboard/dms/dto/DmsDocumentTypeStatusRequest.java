package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotNull;

public record DmsDocumentTypeStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active) {
}
