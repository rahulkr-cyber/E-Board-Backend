package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMasterOptionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DmsMasterConfigurationCodec {

    private static final String STATIC_OPTIONS_KEY = "staticOptions";

    private final ObjectMapper objectMapper;
    private final Environment environment;

    public DmsMasterConfigurationCodec(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    public String write(Map<String, Object> configuration, List<DmsMasterOptionRequest> staticOptions) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (configuration != null) {
            result.putAll(configuration);
        }
        validateSecretValues(result);
        if (staticOptions != null) {
            List<Map<String, Object>> options = new ArrayList<>();
            for (DmsMasterOptionRequest option : staticOptions) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("value", option.value().trim());
                item.put("label", option.label().trim());
                item.put("sortOrder", option.sortOrder() == null ? 0 : option.sortOrder());
                item.put("active", option.active() == null ? Boolean.TRUE : option.active());
                options.add(item);
            }
            result.put(STATIC_OPTIONS_KEY, options);
        } else {
            result.remove(STATIC_OPTIONS_KEY);
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("DMS master source configuration is not valid JSON");
        }
    }

    public Map<String, Object> read(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Stored DMS master source configuration is invalid");
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> staticOptions(String json) {
        Object raw = read(json).get(STATIC_OPTIONS_KEY);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    public Map<String, Object> publicConfiguration(String json) {
        Map<String, Object> config = new LinkedHashMap<>(read(json));
        config.remove(STATIC_OPTIONS_KEY);
        return config;
    }

    public String resolvePlaceholders(String value) {
        return value == null ? null : environment.resolvePlaceholders(value);
    }

    private void validateSecretValues(Map<String, Object> configuration) {
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            if ((key.contains("password") || key.contains("secret") || key.contains("token"))
                    && value instanceof String text
                    && !text.isBlank()
                    && !(text.startsWith("${") && text.endsWith("}"))) {
                throw new ValidationException(
                        "Sensitive master source configuration must use an environment placeholder: "
                                + entry.getKey());
            }
        }
    }
}
