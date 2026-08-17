package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DmsMetadataValidationRequest(
        @NotNull Map<String, Object> values) {
}
