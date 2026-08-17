package com.bor.eboard.dms.dto;

import java.util.List;

public record DmsMetadataConfigurationOptionsResponse(
        List<ControlTypeOption> controlTypes,
        List<CodeLabelOption> dateConstraints,
        List<ConditionOperatorOption> conditionOperators) {

    public record ControlTypeOption(
            String code,
            String label,
            boolean supportsOptions,
            boolean supportsLength,
            boolean supportsNumericRange,
            boolean supportsDateConstraint,
            boolean multipleValues) {
    }

    public record CodeLabelOption(String code, String label) {
    }

    public record ConditionOperatorOption(
            String code,
            String label,
            boolean requiresValue) {
    }
}
