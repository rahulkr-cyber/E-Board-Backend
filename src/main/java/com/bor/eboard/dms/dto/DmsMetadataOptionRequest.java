package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DmsMetadataOptionRequest(
        @NotBlank
        @Size(max = 500)
        String value,

        @NotBlank
        @Size(max = 500)
        String label,

        @Min(0)
        Integer sortOrder,

        Boolean active) {
}
