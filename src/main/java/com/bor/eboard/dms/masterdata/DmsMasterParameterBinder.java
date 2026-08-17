package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DmsMasterParameterBinder {

    public List<BoundParameter> bind(
            List<DmsMasterSourceParameter> definitions,
            Map<String, Object> suppliedValues) {
        Map<String, Object> supplied = suppliedValues == null ? Map.of() : suppliedValues;
        return definitions.stream()
                .filter(parameter -> Boolean.TRUE.equals(parameter.getActive()))
                .map(parameter -> bindOne(parameter, supplied))
                .filter(parameter -> parameter.value() != null)
                .toList();
    }

    public Map<String, Object> byTarget(
            List<BoundParameter> parameters,
            DmsMasterParameterLocation location) {
        Map<String, Object> result = new LinkedHashMap<>();
        parameters.stream()
                .filter(parameter -> parameter.location() == location)
                .forEach(parameter -> result.put(parameter.targetName(), parameter.value()));
        return result;
    }

    private BoundParameter bindOne(
            DmsMasterSourceParameter definition,
            Map<String, Object> supplied) {
        Object raw = supplied.get(definition.getParameterName());
        if (isBlank(raw)) {
            raw = definition.getDefaultValue();
        }
        if (isBlank(raw)) {
            if (Boolean.TRUE.equals(definition.getRequired())) {
                throw new ValidationException(
                        "Required master source parameter is missing: "
                                + definition.getParameterName());
            }
            return new BoundParameter(
                    definition.getParameterName(),
                    definition.getTargetName(),
                    definition.getParameterLocation(),
                    null);
        }
        return new BoundParameter(
                definition.getParameterName(),
                definition.getTargetName(),
                definition.getParameterLocation(),
                convert(raw, definition.getDataType(), definition.getParameterName()));
    }

    private Object convert(Object raw, DmsMasterParameterDataType type, String name) {
        String text = String.valueOf(raw).trim();
        try {
            return switch (type) {
                case STRING -> text;
                case INTEGER -> Long.valueOf(text);
                case DECIMAL -> new BigDecimal(text);
                case BOOLEAN -> parseBoolean(text, name);
                case DATE -> LocalDate.parse(text);
            };
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new ValidationException("Invalid value for master source parameter: " + name);
        }
    }

    private Boolean parseBoolean(String text, String name) {
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.valueOf(text);
        }
        throw new ValidationException("Invalid boolean value for master source parameter: " + name);
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof String text && text.trim().isEmpty());
    }

    public record BoundParameter(
            String parameterName,
            String targetName,
            DmsMasterParameterLocation location,
            Object value) {
    }
}
