package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.masterdata.DmsMasterHttpMethod;
import com.bor.eboard.dms.masterdata.DmsMasterSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record DmsMasterSourceUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull DmsMasterSourceType sourceType,
        @NotBlank @Size(max = 100) String valueField,
        @NotBlank @Size(max = 100) String labelField,
        @Size(max = 500) String responsePath,
        @Size(max = 10000) String queryText,
        @Size(max = 200) String procedureName,
        @Size(max = 2000) String endpointUrl,
        DmsMasterHttpMethod httpMethod,
        Map<String, Object> configuration,
        List<@Valid DmsMasterOptionRequest> staticOptions,
        List<@Valid DmsMasterSourceParameterRequest> parameters,
        @Min(0) @Max(86400) Integer cacheTtlSeconds,
        @Min(1) @Max(1000) Integer maxResults,
        Boolean active) {
}
