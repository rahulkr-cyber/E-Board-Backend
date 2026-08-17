package com.bor.eboard.dms.validation;

import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMetadataConditionRequest;
import com.bor.eboard.dms.dto.DmsMetadataOptionRequest;
import com.bor.eboard.dms.entity.DmsMetadataField;
import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class DmsMetadataDefinitionValidator {

    public void validate(
            UUID documentTypeId,
            String currentFieldKey,
            DmsMetadataControlType controlType,
            Integer minLength,
            Integer maxLength,
            BigDecimal minValue,
            BigDecimal maxValue,
            String regexPattern,
            DmsMetadataDateConstraint dateConstraint,
            DmsMetadataConditionRequest visibilityCondition,
            DmsMetadataConditionRequest mandatoryCondition,
            UUID parentFieldId,
            List<DmsMetadataOptionRequest> options,
            List<DmsMetadataField> existingFields) {

        validateRange("Minimum length", minLength, "maximum length", maxLength);
        validateRange("Minimum value", minValue, "maximum value", maxValue);
        validateRegex(regexPattern);
        validateControlSpecificRules(
                controlType,
                minLength,
                maxLength,
                minValue,
                maxValue,
                regexPattern,
                dateConstraint,
                options);
        validateOptions(options);
        validateCondition(documentTypeId, currentFieldKey, "visibility", visibilityCondition, existingFields);
        validateCondition(documentTypeId, currentFieldKey, "mandatory", mandatoryCondition, existingFields);
        validateParent(documentTypeId, currentFieldKey, parentFieldId, existingFields);
    }

    private void validateControlSpecificRules(
            DmsMetadataControlType controlType,
            Integer minLength,
            Integer maxLength,
            BigDecimal minValue,
            BigDecimal maxValue,
            String regexPattern,
            DmsMetadataDateConstraint dateConstraint,
            List<DmsMetadataOptionRequest> options) {
        if ((minLength != null || maxLength != null
                || (regexPattern != null && !regexPattern.isBlank()))
                && !controlType.supportsLength()) {
            throw new ValidationException(
                    "Length and regular-expression validation are not supported by control type "
                            + controlType);
        }
        if ((minValue != null || maxValue != null) && !controlType.supportsNumericRange()) {
            throw new ValidationException("Numeric range is supported only for Number and Decimal controls");
        }
        if (dateConstraint != null
                && dateConstraint != DmsMetadataDateConstraint.NONE
                && !controlType.supportsDateConstraint()) {
            throw new ValidationException("Date restriction is supported only for Date and Date Time controls");
        }
        if (options != null && !options.isEmpty() && !controlType.supportsOptions()) {
            throw new ValidationException("Static options are not supported by control type " + controlType);
        }
    }

    private void validateOptions(List<DmsMetadataOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        Set<String> values = new HashSet<>();
        for (DmsMetadataOptionRequest option : options) {
            String normalized = option.value().trim().toLowerCase(Locale.ROOT);
            if (!values.add(normalized)) {
                throw new ValidationException("Duplicate metadata option value: " + option.value().trim());
            }
        }
    }

    private void validateCondition(
            UUID documentTypeId,
            String currentFieldKey,
            String conditionName,
            DmsMetadataConditionRequest condition,
            List<DmsMetadataField> existingFields) {
        if (condition == null) {
            return;
        }

        String dependentKey = condition.fieldKey().trim();
        if (dependentKey.equalsIgnoreCase(currentFieldKey)) {
            throw new ValidationException("A metadata field cannot depend on itself for " + conditionName);
        }

        boolean exists = existingFields.stream()
                .anyMatch(field -> field.getDocumentTypeId().equals(documentTypeId)
                        && field.getFieldKey().equalsIgnoreCase(dependentKey)
                        && !Boolean.TRUE.equals(field.getDeleted()));
        if (!exists) {
            throw new ValidationException(
                    "Dependent field does not exist in this document type: " + dependentKey);
        }

        DmsMetadataConditionOperator operator = condition.operator();
        if (operator.requiresValue()
                && (condition.value() == null || condition.value().trim().isEmpty())) {
            throw new ValidationException("Condition value is required for operator " + operator);
        }
    }

    private void validateParent(
            UUID documentTypeId,
            String currentFieldKey,
            UUID parentFieldId,
            List<DmsMetadataField> existingFields) {
        if (parentFieldId == null) {
            return;
        }

        DmsMetadataField parent = existingFields.stream()
                .filter(field -> parentFieldId.equals(field.getId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Parent metadata field does not exist in this document type"));

        if (!parent.getDocumentTypeId().equals(documentTypeId)) {
            throw new ValidationException("Parent metadata field belongs to a different document type");
        }
        if (!Boolean.TRUE.equals(parent.getActive())) {
            throw new ValidationException("Parent metadata field must be active");
        }
        if (parent.getFieldKey().equalsIgnoreCase(currentFieldKey)) {
            throw new ValidationException("A metadata field cannot be its own parent");
        }
    }

    private void validateRegex(String regexPattern) {
        if (regexPattern == null || regexPattern.isBlank()) {
            return;
        }
        try {
            Pattern.compile(regexPattern);
        } catch (PatternSyntaxException ex) {
            throw new ValidationException("Invalid metadata regular expression: " + ex.getDescription());
        }
    }

    private <T extends Comparable<T>> void validateRange(
            String minimumLabel,
            T minimum,
            String maximumLabel,
            T maximum) {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new ValidationException(minimumLabel + " cannot be greater than " + maximumLabel);
        }
    }
}
