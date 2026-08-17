package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMasterConfigurationOptionsResponse;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.dto.DmsMasterOptionRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceCreateRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceParameterRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceTestResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceUpdateRequest;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import com.bor.eboard.dms.masterdata.DmsMasterConfigurationCodec;
import com.bor.eboard.dms.masterdata.DmsMasterHttpMethod;
import com.bor.eboard.dms.masterdata.DmsMasterParameterDataType;
import com.bor.eboard.dms.masterdata.DmsMasterParameterLocation;
import com.bor.eboard.dms.masterdata.DmsMasterSourceResolverRegistry;
import com.bor.eboard.dms.masterdata.DmsMasterSourceType;
import com.bor.eboard.dms.repository.DmsMasterSourceParameterRepository;
import com.bor.eboard.dms.repository.DmsMasterSourceRepository;
import com.bor.eboard.dms.repository.DmsMetadataFieldRepository;
import com.bor.eboard.dms.service.DmsMasterSourceService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class DmsMasterSourceServiceImpl implements DmsMasterSourceService {

    private static final String ENTITY_TYPE = "MASTER_SOURCE";
    private static final Pattern CODE = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,59}$");
    private static final Pattern FIELD_NAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.-]{0,99}$");
    private static final Set<DmsMasterParameterLocation> REST_LOCATIONS =
            EnumSet.of(DmsMasterParameterLocation.QUERY, DmsMasterParameterLocation.PATH,
                    DmsMasterParameterLocation.HEADER, DmsMasterParameterLocation.BODY);

    private final DmsMasterSourceRepository sourceRepository;
    private final DmsMasterSourceParameterRepository parameterRepository;
    private final DmsMetadataFieldRepository metadataFieldRepository;
    private final DmsMasterConfigurationCodec configurationCodec;
    private final DmsMasterSourceResolverRegistry resolverRegistry;
    private final DmsAuditTrailService auditTrailService;
    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public DmsMasterSourceServiceImpl(
            DmsMasterSourceRepository sourceRepository,
            DmsMasterSourceParameterRepository parameterRepository,
            DmsMetadataFieldRepository metadataFieldRepository,
            DmsMasterConfigurationCodec configurationCodec,
            DmsMasterSourceResolverRegistry resolverRegistry,
            DmsAuditTrailService auditTrailService) {
        this.sourceRepository = sourceRepository;
        this.parameterRepository = parameterRepository;
        this.metadataFieldRepository = metadataFieldRepository;
        this.configurationCodec = configurationCodec;
        this.resolverRegistry = resolverRegistry;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsMasterConfigurationOptionsResponse configurationOptions() {
        return new DmsMasterConfigurationOptionsResponse(
                List.of(DmsMasterSourceType.values()).stream()
                        .map(type -> new DmsMasterConfigurationOptionsResponse.CodeLabel(
                                type.name(), type.getLabel()))
                        .toList(),
                List.of(DmsMasterParameterLocation.values()).stream()
                        .map(value -> codeLabel(value.name()))
                        .toList(),
                List.of(DmsMasterParameterDataType.values()).stream()
                        .map(value -> codeLabel(value.name()))
                        .toList(),
                List.of(DmsMasterHttpMethod.values()).stream()
                        .map(value -> codeLabel(value.name()))
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsMasterSourceResponse> findAll(boolean activeOnly) {
        List<DmsMasterSource> sources = activeOnly
                ? sourceRepository.findByActiveTrueAndDeletedFalseOrderByNameAsc()
                : sourceRepository.findByDeletedFalseOrderByNameAsc();
        return sources.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DmsMasterSourceResponse findById(UUID id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public DmsMasterSourceResponse create(DmsMasterSourceCreateRequest request) {
        String code = normalizeCode(request.code());
        String name = requiredText(request.name());
        validateUniqueForCreate(code, name);
        validateDefinition(
                request.sourceType(), request.valueField(), request.labelField(),
                request.queryText(), request.procedureName(), request.endpointUrl(),
                request.httpMethod(), request.configuration(), request.staticOptions(),
                request.parameters());

        DmsMasterSource source = new DmsMasterSource();
        source.setCode(code);
        apply(source, request.name(), request.description(), request.sourceType(),
                request.valueField(), request.labelField(), request.responsePath(),
                request.queryText(), request.procedureName(), request.endpointUrl(),
                request.httpMethod(), request.configuration(), request.staticOptions(),
                request.cacheTtlSeconds(), request.maxResults(), request.active());
        source = save(source);
        replaceParameters(source.getId(), request.parameters());
        auditTrailService.record(ENTITY_TYPE, source.getId(),
                "CREATE", null, auditValue(source));
        return toResponse(source);
    }

    @Override
    @Transactional
    public DmsMasterSourceResponse update(UUID id, DmsMasterSourceUpdateRequest request) {
        DmsMasterSource source = findEntity(id);
        String name = requiredText(request.name());
        if (sourceRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse(name, id)) {
            throw new DuplicateResourceException("DMS master source name already exists: " + name);
        }
        validateDefinition(
                request.sourceType(), request.valueField(), request.labelField(),
                request.queryText(), request.procedureName(), request.endpointUrl(),
                request.httpMethod(), request.configuration(), request.staticOptions(),
                request.parameters());

        String oldValue = auditValue(source);
        apply(source, request.name(), request.description(), request.sourceType(),
                request.valueField(), request.labelField(), request.responsePath(),
                request.queryText(), request.procedureName(), request.endpointUrl(),
                request.httpMethod(), request.configuration(), request.staticOptions(),
                request.cacheTtlSeconds(), request.maxResults(), request.active());
        source = save(source);
        replaceParameters(source.getId(), request.parameters());
        invalidate(source.getId());
        auditTrailService.record(ENTITY_TYPE, source.getId(),
                "UPDATE", oldValue, auditValue(source));
        return toResponse(source);
    }

    @Override
    @Transactional
    public DmsMasterSourceResponse setActive(UUID id, boolean active) {
        DmsMasterSource source = findEntity(id);
        if (!active && metadataFieldRepository
                .existsByMasterSourceIdAndActiveTrueAndDeletedFalse(id)) {
            throw new BusinessException(
                    "Master source is used by an active metadata field and cannot be deactivated");
        }
        String oldValue = auditValue(source);
        source.setActive(active);
        source = sourceRepository.save(source);
        invalidate(id);
        auditTrailService.record(ENTITY_TYPE, id,
                active ? "ACTIVATE" : "DEACTIVATE", oldValue, auditValue(source));
        return toResponse(source);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DmsMasterSource source = findEntity(id);
        if (metadataFieldRepository.existsByMasterSourceIdAndDeletedFalse(id)) {
            throw new BusinessException(
                    "Master source is used by metadata and cannot be deleted");
        }
        String oldValue = auditValue(source);
        source.setDeleted(Boolean.TRUE);
        sourceRepository.save(source);
        List<DmsMasterSourceParameter> parameters = parameters(id);
        parameters.forEach(parameter -> parameter.setDeleted(Boolean.TRUE));
        parameterRepository.saveAll(parameters);
        invalidate(id);
        auditTrailService.record(ENTITY_TYPE, id,
                "DELETE", oldValue, "soft-deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsMasterDataOptionResponse> resolve(
            UUID id,
            Map<String, Object> parameters) {
        DmsMasterSource source = findEntity(id);
        if (!Boolean.TRUE.equals(source.getActive())) {
            throw new BusinessException("DMS master source is inactive");
        }
        Map<String, Object> safeParameters = parameters == null
                ? Map.of()
                : new LinkedHashMap<>(parameters);
        int ttl = source.getCacheTtlSeconds() == null ? 0 : source.getCacheTtlSeconds();
        CacheKey key = new CacheKey(id, new java.util.TreeMap<>(safeParameters).toString());
        if (ttl > 0) {
            CacheEntry existing = cache.get(key);
            if (existing != null && existing.expiresAt().isAfter(LocalDateTime.now())) {
                return existing.options();
            }
        }
        List<DmsMasterDataOptionResponse> options = List.copyOf(
                resolverRegistry.get(source.getSourceType())
                        .resolve(source, parameters(id), safeParameters));
        if (ttl > 0) {
            cache.put(key, new CacheEntry(options, LocalDateTime.now().plusSeconds(ttl)));
        }
        return options;
    }

    @Override
    @Transactional
    public DmsMasterSourceTestResponse test(UUID id, Map<String, Object> parameters) {
        List<DmsMasterDataOptionResponse> options = resolve(id, parameters);
        auditTrailService.record(ENTITY_TYPE, id,
                "TEST", null, "resultCount=" + options.size());
        return new DmsMasterSourceTestResponse(
                true, options.size(), options, LocalDateTime.now());
    }

    private void apply(
            DmsMasterSource source,
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
            List<DmsMasterOptionRequest> staticOptions,
            Integer cacheTtlSeconds,
            Integer maxResults,
            Boolean active) {
        source.setName(requiredText(name));
        source.setDescription(optionalText(description));
        source.setSourceType(sourceType);
        source.setValueField(requiredText(valueField));
        source.setLabelField(requiredText(labelField));
        source.setResponsePath(optionalText(responsePath));
        source.setQueryText(optionalText(queryText));
        source.setProcedureName(optionalText(procedureName));
        source.setEndpointUrl(optionalText(endpointUrl));
        source.setHttpMethod(httpMethod == null ? DmsMasterHttpMethod.GET : httpMethod);
        source.setConfigurationJson(configurationCodec.write(configuration, staticOptions));
        source.setCacheTtlSeconds(cacheTtlSeconds == null ? 0 : cacheTtlSeconds);
        source.setMaxResults(maxResults == null ? 500 : maxResults);
        source.setActive(active == null ? Boolean.TRUE : active);
    }

    private void replaceParameters(
            UUID sourceId,
            List<DmsMasterSourceParameterRequest> requests) {
        List<DmsMasterSourceParameter> existing = parameters(sourceId);
        existing.forEach(parameter -> parameter.setDeleted(Boolean.TRUE));
        parameterRepository.saveAll(existing);
        parameterRepository.flush();

        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<DmsMasterSourceParameter> replacements = new ArrayList<>();
        for (DmsMasterSourceParameterRequest request : requests) {
            DmsMasterSourceParameter parameter = new DmsMasterSourceParameter();
            parameter.setMasterSourceId(sourceId);
            parameter.setParameterName(request.parameterName().trim());
            parameter.setTargetName(request.targetName().trim());
            parameter.setParameterLocation(request.parameterLocation());
            parameter.setDataType(request.dataType());
            parameter.setRequired(request.required() == null ? Boolean.FALSE : request.required());
            parameter.setDefaultValue(optionalText(request.defaultValue()));
            parameter.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
            parameter.setActive(request.active() == null ? Boolean.TRUE : request.active());
            replacements.add(parameter);
        }
        parameterRepository.saveAll(replacements);
    }

    private void validateDefinition(
            DmsMasterSourceType sourceType,
            String valueField,
            String labelField,
            String queryText,
            String procedureName,
            String endpointUrl,
            DmsMasterHttpMethod httpMethod,
            Map<String, Object> configuration,
            List<DmsMasterOptionRequest> staticOptions,
            List<DmsMasterSourceParameterRequest> parameters) {
        validateFieldName(valueField, "Value field");
        validateFieldName(labelField, "Label field");
        validateParameters(sourceType, parameters);
        validateStaticOptions(staticOptions);

        switch (sourceType) {
            case STATIC_LIST -> {
                if (staticOptions == null || staticOptions.isEmpty()) {
                    throw new ValidationException("Static master source requires at least one option");
                }
            }
            case DATABASE_QUERY -> {
                if (queryText == null || queryText.isBlank()) {
                    throw new ValidationException("Database query is required");
                }
                String normalizedQuery = queryText.stripLeading().toLowerCase(Locale.ROOT);
                if (!(normalizedQuery.startsWith("select ") || normalizedQuery.startsWith("with "))
                        || normalizedQuery.contains(";")
                        || normalizedQuery.contains("--")
                        || normalizedQuery.contains("/*")) {
                    throw new ValidationException(
                            "Database master source must contain one read-only SELECT/WITH statement");
                }
            }
            case STORED_PROCEDURE -> {
                if (procedureName == null || procedureName.isBlank()) {
                    throw new ValidationException("Stored procedure name is required");
                }
                if (!procedureName.matches(
                        "^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$")) {
                    throw new ValidationException("Stored procedure name is invalid");
                }
            }
            case REST_API, GOVERNMENT_API -> {
                if (endpointUrl == null || endpointUrl.isBlank()) {
                    throw new ValidationException("Endpoint URL is required");
                }
                if (!(endpointUrl.startsWith("https://") || endpointUrl.startsWith("http://"))) {
                    throw new ValidationException("Endpoint URL must use HTTP or HTTPS");
                }
                if (httpMethod == null) {
                    throw new ValidationException("HTTP method is required");
                }
            }
            case LDAP -> {
                Map<String, Object> config = configuration == null ? Map.of() : configuration;
                requireConfiguration(config, "providerUrl");
                requireConfiguration(config, "baseDn");
                requireConfiguration(config, "filter");
            }
        }
    }

    private void validateParameters(
            DmsMasterSourceType sourceType,
            List<DmsMasterSourceParameterRequest> parameters) {
        if (parameters == null) {
            return;
        }
        Set<String> names = new HashSet<>();
        Set<String> targets = new HashSet<>();
        for (DmsMasterSourceParameterRequest parameter : parameters) {
            String name = parameter.parameterName().trim().toLowerCase(Locale.ROOT);
            String target = parameter.parameterLocation() + ":"
                    + parameter.targetName().trim().toLowerCase(Locale.ROOT);
            if (!names.add(name)) {
                throw new ValidationException("Duplicate master source parameter: "
                        + parameter.parameterName());
            }
            if (!targets.add(target)) {
                throw new ValidationException("Duplicate master source target: "
                        + parameter.targetName());
            }
            boolean validLocation = switch (sourceType) {
                case DATABASE_QUERY, STORED_PROCEDURE ->
                        parameter.parameterLocation() == DmsMasterParameterLocation.SQL;
                case REST_API, GOVERNMENT_API ->
                        REST_LOCATIONS.contains(parameter.parameterLocation());
                case LDAP -> parameter.parameterLocation() == DmsMasterParameterLocation.LDAP_FILTER;
                case STATIC_LIST -> false;
            };
            if (!validLocation) {
                throw new ValidationException("Parameter location "
                        + parameter.parameterLocation() + " is not valid for " + sourceType);
            }
        }
    }

    private void validateStaticOptions(List<DmsMasterOptionRequest> options) {
        if (options == null) {
            return;
        }
        Set<String> values = new HashSet<>();
        for (DmsMasterOptionRequest option : options) {
            if (!values.add(option.value().trim().toLowerCase(Locale.ROOT))) {
                throw new ValidationException("Duplicate static option value: " + option.value());
            }
        }
    }

    private void validateFieldName(String value, String label) {
        if (value == null || !FIELD_NAME.matcher(value.trim()).matches()) {
            throw new ValidationException(label + " is invalid");
        }
    }

    private void requireConfiguration(Map<String, Object> configuration, String key) {
        Object value = configuration.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ValidationException("LDAP configuration is missing: " + key);
        }
    }

    private DmsMasterSource save(DmsMasterSource source) {
        try {
            return sourceRepository.saveAndFlush(source);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "A DMS master source with the same code or name already exists");
        }
    }

    private void validateUniqueForCreate(String code, String name) {
        if (sourceRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            throw new DuplicateResourceException("DMS master source code already exists: " + code);
        }
        if (sourceRepository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new DuplicateResourceException("DMS master source name already exists: " + name);
        }
    }

    private DmsMasterSource findEntity(UUID id) {
        return sourceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DMS master source", id));
    }

    private List<DmsMasterSourceParameter> parameters(UUID sourceId) {
        return parameterRepository
                .findByMasterSourceIdAndDeletedFalseOrderBySortOrderAscParameterNameAsc(sourceId);
    }

    private DmsMasterSourceResponse toResponse(DmsMasterSource source) {
        List<DmsMasterSourceResponse.Option> options =
                configurationCodec.staticOptions(source.getConfigurationJson()).stream()
                        .sorted(Comparator.comparingInt(item -> integerValue(item.get("sortOrder"))))
                        .map(item -> new DmsMasterSourceResponse.Option(
                                String.valueOf(item.get("value")),
                                String.valueOf(item.get("label")),
                                integerValue(item.get("sortOrder")),
                                !Boolean.FALSE.equals(item.get("active"))))
                        .toList();
        List<DmsMasterSourceResponse.Parameter> parameterResponses = parameters(source.getId()).stream()
                .map(parameter -> new DmsMasterSourceResponse.Parameter(
                        parameter.getId(), parameter.getParameterName(), parameter.getTargetName(),
                        parameter.getParameterLocation(), parameter.getDataType(),
                        parameter.getRequired(), parameter.getDefaultValue(),
                        parameter.getSortOrder(), parameter.getActive()))
                .toList();
        return new DmsMasterSourceResponse(
                source.getId(), source.getCode(), source.getName(), source.getDescription(),
                source.getSourceType(), source.getValueField(), source.getLabelField(),
                source.getResponsePath(), source.getQueryText(), source.getProcedureName(),
                source.getEndpointUrl(), source.getHttpMethod(),
                configurationCodec.publicConfiguration(source.getConfigurationJson()),
                options, parameterResponses, source.getCacheTtlSeconds(), source.getMaxResults(),
                source.getActive(), source.getCreatedAt(), source.getUpdatedAt());
    }

    private DmsMasterConfigurationOptionsResponse.CodeLabel codeLabel(String code) {
        return new DmsMasterConfigurationOptionsResponse.CodeLabel(
                code, code.replace('_', ' '));
    }

    private String normalizeCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new ValidationException("DMS master source code is invalid");
        }
        return code;
    }

    private String requiredText(String value) {
        return value == null ? "" : value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String auditValue(DmsMasterSource source) {
        return "code=" + source.getCode()
                + ",name=" + source.getName()
                + ",type=" + source.getSourceType()
                + ",active=" + source.getActive();
    }

    private void invalidate(UUID sourceId) {
        cache.keySet().removeIf(key -> Objects.equals(key.sourceId(), sourceId));
    }

    private record CacheKey(UUID sourceId, String parameters) {
    }

    private record CacheEntry(
            List<DmsMasterDataOptionResponse> options,
            LocalDateTime expiresAt) {
    }
}
