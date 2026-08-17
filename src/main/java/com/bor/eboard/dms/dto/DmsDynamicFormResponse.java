package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Runtime form definition consumed by the Angular dynamic form renderer.
 */
public record DmsDynamicFormResponse(
        UUID documentTypeId,
        String documentTypeCode,
        String documentTypeName,
        String documentTypeDescription,
        List<Field> fields) {

    public record Field(
            UUID id,
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
            Integer minLength,
            Integer maxLength,
            BigDecimal minValue,
            BigDecimal maxValue,
            String regexPattern,
            DmsMetadataDateConstraint dateConstraint,
            Condition visibilityCondition,
            Condition mandatoryCondition,
            UUID masterSourceId,
            String parentFieldKey,
            String sourceParameterName,
            List<Option> options) {
    }

    public record Condition(
            String fieldKey,
            DmsMetadataConditionOperator operator,
            String value) {
    }

    public record Option(String value, String label) {
    }
}
