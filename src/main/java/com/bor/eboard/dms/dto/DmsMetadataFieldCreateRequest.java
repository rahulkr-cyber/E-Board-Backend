package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DmsMetadataFieldCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,79}$",
                message = "Field key must start with a letter and contain only letters, digits or underscores")
        String fieldKey,

        @NotBlank
        @Size(max = 150)
        String label,

        @NotNull
        DmsMetadataControlType controlType,

        @Size(max = 250)
        String placeholder,

        @Size(max = 1000)
        String helpText,

        @Size(max = 4000)
        String defaultValue,

        @Min(0)
        Integer sortOrder,

        Boolean required,
        Boolean readOnly,
        Boolean hidden,
        Boolean searchable,
        Boolean active,

        @Min(0)
        Integer minLength,

        @Min(0)
        Integer maxLength,

        BigDecimal minValue,
        BigDecimal maxValue,

        @Size(max = 1000)
        String regexPattern,

        DmsMetadataDateConstraint dateConstraint,

        @Valid
        DmsMetadataConditionRequest visibilityCondition,

        @Valid
        DmsMetadataConditionRequest mandatoryCondition,

        UUID masterSourceId,
        UUID parentFieldId,

        @Size(max = 80)
        String sourceParameterName,

        List<@Valid DmsMetadataOptionRequest> options) {
}
