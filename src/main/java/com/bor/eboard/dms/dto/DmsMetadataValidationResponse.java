package com.bor.eboard.dms.dto;

import java.util.List;

public record DmsMetadataValidationResponse(
        boolean valid,
        List<FieldError> errors) {

    public record FieldError(
            String fieldKey,
            String label,
            String message) {
    }
}
