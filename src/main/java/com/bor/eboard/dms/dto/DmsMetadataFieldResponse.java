package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DmsMetadataFieldResponse(
        UUID id,
        UUID documentTypeId,
        String fieldKey,
        String label,
        DmsMetadataControlType controlType,
        String placeholder,
        String helpText,
        String defaultValue,
        Integer sortOrder,
        Boolean required,
        Boolean readOnly,
        Boolean hidden,
        Boolean searchable,
        Boolean active,
        Integer minLength,
        Integer maxLength,
        BigDecimal minValue,
        BigDecimal maxValue,
        String regexPattern,
        DmsMetadataDateConstraint dateConstraint,
        Condition visibilityCondition,
        Condition mandatoryCondition,
        UUID masterSourceId,
        UUID parentFieldId,
        String sourceParameterName,
        List<Option> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public record Condition(
            String fieldKey,
            DmsMetadataConditionOperator operator,
            String value) {
    }

    public record Option(
            UUID id,
            String value,
            String label,
            Integer sortOrder,
            Boolean active) {
    }
}
