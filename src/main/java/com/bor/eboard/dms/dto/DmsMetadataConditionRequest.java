package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DmsMetadataConditionRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,79}$",
                message = "Dependent field key must start with a letter and contain only letters, digits or underscores")
        String fieldKey,

        @NotNull
        DmsMetadataConditionOperator operator,

        @Size(max = 2000)
        String value) {
}
