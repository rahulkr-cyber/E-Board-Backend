package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.dms.dto.DmsMetadataValidationResponse;
import com.bor.eboard.dms.entity.DmsMetadataField;
import com.bor.eboard.dms.entity.DmsMetadataOption;
import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;
import com.bor.eboard.dms.repository.DmsDocumentTypeRepository;
import com.bor.eboard.dms.repository.DmsMetadataFieldRepository;
import com.bor.eboard.dms.repository.DmsMetadataOptionRepository;
import com.bor.eboard.dms.service.DmsMetadataValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DmsMetadataValidationServiceImpl implements DmsMetadataValidationService {

    private final DmsMetadataFieldRepository fieldRepository;
    private final DmsMetadataOptionRepository optionRepository;
    private final DmsDocumentTypeRepository documentTypeRepository;

    public DmsMetadataValidationServiceImpl(
            DmsMetadataFieldRepository fieldRepository,
            DmsMetadataOptionRepository optionRepository,
            DmsDocumentTypeRepository documentTypeRepository) {
        this.fieldRepository = fieldRepository;
        this.optionRepository = optionRepository;
        this.documentTypeRepository = documentTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsMetadataValidationResponse validate(
            UUID documentTypeId,
            Map<String, Object> values) {
        documentTypeRepository.findByIdAndDeletedFalse(documentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document type", documentTypeId));

        List<DmsMetadataField> fields = fieldRepository
                .findByDocumentTypeIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscLabelAsc(documentTypeId);
        Map<String, DmsMetadataField> fieldByKey = fields.stream()
                .collect(Collectors.toMap(
                        field -> field.getFieldKey().toLowerCase(Locale.ROOT),
                        field -> field));

        List<DmsMetadataValidationResponse.FieldError> errors = new ArrayList<>();
        for (String submittedKey : values.keySet()) {
            if (!fieldByKey.containsKey(submittedKey.toLowerCase(Locale.ROOT))) {
                errors.add(new DmsMetadataValidationResponse.FieldError(
                        submittedKey,
                        submittedKey,
                        "Unknown or inactive metadata field"));
            }
        }

        Map<UUID, List<DmsMetadataOption>> optionsByField = loadOptions(fields);
        for (DmsMetadataField field : fields) {
            validateField(field, values,
                    optionsByField.getOrDefault(field.getId(), List.of()), errors);
        }

        return new DmsMetadataValidationResponse(errors.isEmpty(), List.copyOf(errors));
    }

    private void validateField(
            DmsMetadataField field,
            Map<String, Object> values,
            List<DmsMetadataOption> options,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        if (!isVisible(field, values)) {
            return;
        }

        Object value = findValue(values, field.getFieldKey());
        if (isEmpty(value) && field.getDefaultValue() != null) {
            value = field.getDefaultValue();
        }

        boolean mandatory = Boolean.TRUE.equals(field.getRequired())
                || evaluateCondition(
                        field.getMandatoryConditionFieldKey(),
                        field.getMandatoryConditionOperator(),
                        field.getMandatoryConditionValue(),
                        values);

        if (isEmpty(value)) {
            if (mandatory) {
                addError(errors, field, "Value is required");
            }
            return;
        }

        try {
            validateByControl(field, value, options, errors);
        } catch (IllegalArgumentException ex) {
            addError(errors, field, ex.getMessage());
        }
    }

    private void validateByControl(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataOption> options,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        DmsMetadataControlType type = field.getControlType();
        switch (type) {
            case NUMBER -> validateNumber(field, value, true, errors);
            case DECIMAL -> validateNumber(field, value, false, errors);
            case DATE -> validateDate(field, value, errors);
            case DATE_TIME -> validateDateTime(field, value, errors);
            case CHECKBOX -> validateBoolean(field, value, errors);
            case DROPDOWN, RADIO, AUTOCOMPLETE -> {
                validateText(field, String.valueOf(value), errors);
                validateSingleOption(field, value, options, errors);
            }
            case MULTI_SELECT -> validateMultipleOptions(field, value, options, errors);
            case TAG_INPUT -> validateTags(field, value, errors);
            case TEXTBOX, TEXTAREA, HIDDEN -> validateText(field, String.valueOf(value), errors);
            case FILE_UPLOAD -> {
                // File payload validation belongs to the document upload phase.
            }
        }
    }

    private void validateText(
            DmsMetadataField field,
            String value,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        if (field.getMinLength() != null && value.length() < field.getMinLength()) {
            addError(errors, field,
                    "Minimum length is " + field.getMinLength());
        }
        if (field.getMaxLength() != null && value.length() > field.getMaxLength()) {
            addError(errors, field,
                    "Maximum length is " + field.getMaxLength());
        }
        if (field.getRegexPattern() != null
                && !Pattern.compile(field.getRegexPattern()).matcher(value).matches()) {
            addError(errors, field, "Value does not match the configured format");
        }
    }

    private void validateNumber(
            DmsMetadataField field,
            Object value,
            boolean integerOnly,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        BigDecimal number;
        try {
            number = new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            addError(errors, field, "Value must be numeric");
            return;
        }

        if (integerOnly && number.stripTrailingZeros().scale() > 0) {
            addError(errors, field, "Value must be a whole number");
        }
        if (field.getMinValue() != null && number.compareTo(field.getMinValue()) < 0) {
            addError(errors, field,
                    "Minimum value is " + field.getMinValue().stripTrailingZeros().toPlainString());
        }
        if (field.getMaxValue() != null && number.compareTo(field.getMaxValue()) > 0) {
            addError(errors, field,
                    "Maximum value is " + field.getMaxValue().stripTrailingZeros().toPlainString());
        }
    }

    private void validateDate(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        try {
            LocalDate date = LocalDate.parse(String.valueOf(value));
            validateDateConstraint(field, date, errors);
        } catch (DateTimeParseException ex) {
            addError(errors, field, "Value must be an ISO date (yyyy-MM-dd)");
        }
    }

    private void validateDateTime(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        try {
            LocalDate date;
            String text = String.valueOf(value);
            try {
                date = OffsetDateTime.parse(text).toLocalDate();
            } catch (DateTimeParseException ignored) {
                date = LocalDateTime.parse(text).toLocalDate();
            }
            validateDateConstraint(field, date, errors);
        } catch (DateTimeParseException ex) {
            addError(errors, field, "Value must be an ISO date-time");
        }
    }

    private void validateDateConstraint(
            DmsMetadataField field,
            LocalDate value,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        LocalDate today = LocalDate.now();
        DmsMetadataDateConstraint constraint = field.getDateConstraint();
        boolean valid = switch (constraint) {
            case NONE -> true;
            case PAST -> value.isBefore(today);
            case PAST_OR_PRESENT -> !value.isAfter(today);
            case FUTURE -> value.isAfter(today);
            case FUTURE_OR_PRESENT -> !value.isBefore(today);
        };
        if (!valid) {
            addError(errors, field, constraint.getLabel() + " is required");
        }
    }

    private void validateBoolean(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        if (value instanceof Boolean) {
            return;
        }
        String text = String.valueOf(value);
        if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
            addError(errors, field, "Value must be true or false");
        }
    }

    private void validateSingleOption(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataOption> options,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        if (options.isEmpty() || field.getMasterSourceId() != null) {
            return;
        }
        Set<String> allowed = activeOptionValues(options);
        if (!allowed.contains(String.valueOf(value).toLowerCase(Locale.ROOT))) {
            addError(errors, field, "Value is not one of the configured options");
        }
    }

    private void validateMultipleOptions(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataOption> options,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        List<Object> submitted = toList(value);
        if (submitted.isEmpty()) {
            return;
        }
        if (options.isEmpty() || field.getMasterSourceId() != null) {
            return;
        }
        Set<String> allowed = activeOptionValues(options);
        for (Object item : submitted) {
            if (!allowed.contains(String.valueOf(item).toLowerCase(Locale.ROOT))) {
                addError(errors, field,
                        "Value is not one of the configured options: " + item);
            }
        }
    }

    private void validateTags(
            DmsMetadataField field,
            Object value,
            List<DmsMetadataValidationResponse.FieldError> errors) {
        for (Object tag : toList(value)) {
            validateText(field, String.valueOf(tag), errors);
        }
    }

    private boolean isVisible(DmsMetadataField field, Map<String, Object> values) {
        if (field.getControlType() == DmsMetadataControlType.HIDDEN
                || Boolean.TRUE.equals(field.getHidden())) {
            return true;
        }
        if (field.getVisibilityConditionFieldKey() == null) {
            return true;
        }
        return evaluateCondition(
                field.getVisibilityConditionFieldKey(),
                field.getVisibilityConditionOperator(),
                field.getVisibilityConditionValue(),
                values);
    }

    private boolean evaluateCondition(
            String dependentFieldKey,
            DmsMetadataConditionOperator operator,
            String expected,
            Map<String, Object> values) {
        if (dependentFieldKey == null || operator == null) {
            return false;
        }
        Object actual = findValue(values, dependentFieldKey);
        String actualText = isEmpty(actual) ? "" : String.valueOf(actual).trim();
        return switch (operator) {
            case EQUALS -> actualText.equals(expected == null ? "" : expected);
            case NOT_EQUALS -> !actualText.equals(expected == null ? "" : expected);
            case IN -> splitExpected(expected).contains(actualText);
            case NOT_IN -> !splitExpected(expected).contains(actualText);
            case EMPTY -> isEmpty(actual);
            case NOT_EMPTY -> !isEmpty(actual);
            case TRUE -> "true".equalsIgnoreCase(actualText);
            case FALSE -> "false".equalsIgnoreCase(actualText);
        };
    }

    private Set<String> splitExpected(String expected) {
        if (expected == null || expected.isBlank()) {
            return Set.of();
        }
        Set<String> values = new HashSet<>();
        for (String item : expected.split(",")) {
            values.add(item.trim());
        }
        return values;
    }

    private Map<UUID, List<DmsMetadataOption>> loadOptions(List<DmsMetadataField> fields) {
        if (fields.isEmpty()) {
            return Map.of();
        }
        return optionRepository
                .findByMetadataFieldIdInAndDeletedFalseOrderBySortOrderAscLabelAsc(
                        fields.stream().map(DmsMetadataField::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(DmsMetadataOption::getMetadataFieldId));
    }

    private Set<String> activeOptionValues(List<DmsMetadataOption> options) {
        return options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .map(option -> option.getValue().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Object findValue(Map<String, Object> values, String fieldKey) {
        if (values.containsKey(fieldKey)) {
            return values.get(fieldKey);
        }
        return values.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(fieldKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return value.getClass().isArray() && Array.getLength(value) == 0;
    }

    private List<Object> toList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value != null && value.getClass().isArray()) {
            List<Object> items = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                items.add(Array.get(value, i));
            }
            return items;
        }
        if (value instanceof String text && text.contains(",")) {
            return List.of(text.split(",")).stream()
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .map(item -> (Object) item)
                    .toList();
        }
        return value == null ? List.of() : List.of(value);
    }

    private void addError(
            List<DmsMetadataValidationResponse.FieldError> errors,
            DmsMetadataField field,
            String message) {
        errors.add(new DmsMetadataValidationResponse.FieldError(
                field.getFieldKey(), field.getLabel(), message));
    }
}
