package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.masterdata.DmsMasterHttpMethod;
import com.bor.eboard.dms.masterdata.DmsMasterParameterDataType;
import com.bor.eboard.dms.masterdata.DmsMasterParameterLocation;
import com.bor.eboard.dms.masterdata.DmsMasterSourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DmsMasterSourceResponse(
        UUID id,
        String code,
        String name,
        String description,
        DmsMasterSourceType sourceType,
        String valueField,
        String labelField,
        String responsePath,
        String queryText,
        String procedureName,
        String endpointUrl,
        DmsMasterHttpMethod httpMethod,
        Map<String, Object> configuration,
        List<Option> staticOptions,
        List<Parameter> parameters,
        Integer cacheTtlSeconds,
        Integer maxResults,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public record Option(String value, String label, Integer sortOrder, Boolean active) {
    }

    public record Parameter(
            UUID id,
            String parameterName,
            String targetName,
            DmsMasterParameterLocation parameterLocation,
            DmsMasterParameterDataType dataType,
            Boolean required,
            String defaultValue,
            Integer sortOrder,
            Boolean active) {
    }
}
