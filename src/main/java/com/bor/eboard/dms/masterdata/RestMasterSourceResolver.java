package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RestMasterSourceResolver implements DmsMasterSourceResolver {

    private final ObjectMapper objectMapper;
    private final DmsMasterConfigurationCodec configurationCodec;
    private final DmsMasterParameterBinder parameterBinder;
    private final DmsMasterOptionMapper optionMapper;

    public RestMasterSourceResolver(
            ObjectMapper objectMapper,
            DmsMasterConfigurationCodec configurationCodec,
            DmsMasterParameterBinder parameterBinder,
            DmsMasterOptionMapper optionMapper) {
        this.objectMapper = objectMapper;
        this.configurationCodec = configurationCodec;
        this.parameterBinder = parameterBinder;
        this.optionMapper = optionMapper;
    }

    @Override
    public Set<DmsMasterSourceType> supportedTypes() {
        return Set.of(DmsMasterSourceType.REST_API, DmsMasterSourceType.GOVERNMENT_API);
    }

    @Override
    public List<DmsMasterDataOptionResponse> resolve(
            DmsMasterSource source,
            List<DmsMasterSourceParameter> parameterDefinitions,
            Map<String, Object> parameters) {
        List<DmsMasterParameterBinder.BoundParameter> bound =
                parameterBinder.bind(parameterDefinitions, parameters);
        URI uri = buildUri(source, bound);
        Map<String, Object> headers =
                parameterBinder.byTarget(bound, DmsMasterParameterLocation.HEADER);
        Map<String, Object> body =
                parameterBinder.byTarget(bound, DmsMasterParameterLocation.BODY);

        String response = source.getHttpMethod() == DmsMasterHttpMethod.POST
                ? executePost(uri, headers, body)
                : executeGet(uri, headers);
        return optionMapper.mapRows(source, parseRows(response, source.getResponsePath()));
    }

    private URI buildUri(
            DmsMasterSource source,
            List<DmsMasterParameterBinder.BoundParameter> parameters) {
        String endpoint = configurationCodec.resolvePlaceholders(source.getEndpointUrl());
        if (endpoint == null || endpoint.isBlank()) {
            throw new ValidationException("Endpoint URL is required for this master source");
        }
        if (!(endpoint.startsWith("https://") || endpoint.startsWith("http://"))) {
            throw new ValidationException("Master source endpoint must use HTTP or HTTPS");
        }

        for (DmsMasterParameterBinder.BoundParameter parameter : parameters) {
            if (parameter.location() == DmsMasterParameterLocation.PATH) {
                endpoint = endpoint.replace(
                        "{" + parameter.targetName() + "}",
                        String.valueOf(parameter.value()));
            }
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
        parameterBinder.byTarget(parameters, DmsMasterParameterLocation.QUERY)
                .forEach(builder::queryParam);
        return builder.build().encode().toUri();
    }

    private String executeGet(URI uri, Map<String, Object> headers) {
        try {
            return RestClient.create().get()
                    .uri(uri)
                    .headers(httpHeaders -> headers.forEach(
                            (name, value) -> httpHeaders.set(
                                    name,
                                    configurationCodec.resolvePlaceholders(String.valueOf(value)))))
                    .retrieve()
                    .body(String.class);
        } catch (RuntimeException ex) {
            throw new BusinessException("Unable to resolve REST master source: " + ex.getMessage());
        }
    }

    private String executePost(URI uri, Map<String, Object> headers, Map<String, Object> body) {
        try {
            return RestClient.create().post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(
                            (name, value) -> httpHeaders.set(
                                    name,
                                    configurationCodec.resolvePlaceholders(String.valueOf(value)))))
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RuntimeException ex) {
            throw new BusinessException("Unable to resolve REST master source: " + ex.getMessage());
        }
    }

    private List<Map<String, Object>> parseRows(String response, String responsePath) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(response);
            if (responsePath != null && !responsePath.isBlank()) {
                for (String segment : responsePath.split("\\.")) {
                    node = node.path(segment);
                }
            }
            if (!node.isArray()) {
                throw new BusinessException("Configured REST response path is not an array");
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isObject()) {
                    rows.add(objectMapper.convertValue(
                            item,
                            new TypeReference<LinkedHashMap<String, Object>>() { }));
                }
            }
            return rows;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("REST master source returned invalid JSON");
        }
    }
}
