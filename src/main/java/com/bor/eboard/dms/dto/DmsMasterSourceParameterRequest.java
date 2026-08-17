package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.masterdata.DmsMasterParameterDataType;
import com.bor.eboard.dms.masterdata.DmsMasterParameterLocation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DmsMasterSourceParameterRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,79}$")
        String parameterName,
        @NotBlank @Size(max = 120) String targetName,
        @NotNull DmsMasterParameterLocation parameterLocation,
        @NotNull DmsMasterParameterDataType dataType,
        Boolean required,
        @Size(max = 2000) String defaultValue,
        @Min(0) Integer sortOrder,
        Boolean active) {
}
