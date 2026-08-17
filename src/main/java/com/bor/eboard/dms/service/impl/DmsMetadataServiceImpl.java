package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.dms.dto.DmsMetadataConditionRequest;
import com.bor.eboard.dms.dto.DmsMetadataConfigurationOptionsResponse;
import com.bor.eboard.dms.dto.DmsMetadataFieldCreateRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldOrderRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldResponse;
import com.bor.eboard.dms.dto.DmsMetadataFieldUpdateRequest;
import com.bor.eboard.dms.dto.DmsMetadataOptionRequest;
import com.bor.eboard.dms.entity.DmsMetadataField;
import com.bor.eboard.dms.entity.DmsMetadataOption;
import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;
import com.bor.eboard.dms.repository.DmsDocumentTypeRepository;
import com.bor.eboard.dms.repository.DmsMasterSourceParameterRepository;
import com.bor.eboard.dms.repository.DmsMasterSourceRepository;
import com.bor.eboard.dms.repository.DmsMetadataFieldRepository;
import com.bor.eboard.dms.repository.DmsMetadataOptionRepository;
import com.bor.eboard.dms.service.DmsMetadataService;
import com.bor.eboard.dms.validation.DmsMetadataDefinitionValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DmsMetadataServiceImpl implements DmsMetadataService {

    private static final String ENTITY_TYPE = "METADATA_FIELD";

    private final DmsMetadataFieldRepository fieldRepository;
    private final DmsMetadataOptionRepository optionRepository;
    private final DmsDocumentTypeRepository documentTypeRepository;
    private final DmsMasterSourceRepository masterSourceRepository;
    private final DmsMasterSourceParameterRepository masterSourceParameterRepository;
    private final DmsMetadataDefinitionValidator definitionValidator;
    private final DmsAuditTrailService auditTrailService;

    public DmsMetadataServiceImpl(
            DmsMetadataFieldRepository fieldRepository,
            DmsMetadataOptionRepository optionRepository,
            DmsDocumentTypeRepository documentTypeRepository,
            DmsMasterSourceRepository masterSourceRepository,
            DmsMasterSourceParameterRepository masterSourceParameterRepository,
            DmsMetadataDefinitionValidator definitionValidator,
            DmsAuditTrailService auditTrailService) {
        this.fieldRepository = fieldRepository;
        this.optionRepository = optionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.masterSourceRepository = masterSourceRepository;
        this.masterSourceParameterRepository = masterSourceParameterRepository;
        this.definitionValidator = definitionValidator;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsMetadataConfigurationOptionsResponse configurationOptions() {
        List<DmsMetadataConfigurationOptionsResponse.ControlTypeOption> controls =
                List.of(DmsMetadataControlType.values()).stream()
                        .map(type -> new DmsMetadataConfigurationOptionsResponse.ControlTypeOption(
                                type.name(),
                                type.getLabel(),
                                type.supportsOptions(),
                                type.supportsLength(),
                                type.supportsNumericRange(),
                                type.supportsDateConstraint(),
                                type.supportsMultipleValues()))
                        .toList();

        List<DmsMetadataConfigurationOptionsResponse.CodeLabelOption> dateConstraints =
                List.of(DmsMetadataDateConstraint.values()).stream()
                        .map(value -> new DmsMetadataConfigurationOptionsResponse.CodeLabelOption(
                                value.name(), value.getLabel()))
                        .toList();

        List<DmsMetadataConfigurationOptionsResponse.ConditionOperatorOption> operators =
                List.of(DmsMetadataConditionOperator.values()).stream()
                        .map(value -> new DmsMetadataConfigurationOptionsResponse.ConditionOperatorOption(
                                value.name(), value.getLabel(), value.requiresValue()))
                        .toList();

        return new DmsMetadataConfigurationOptionsResponse(controls, dateConstraints, operators);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsMetadataFieldResponse> findAll(UUID documentTypeId, boolean activeOnly) {
        requireDocumentType(documentTypeId);
        List<DmsMetadataField> fields = activeOnly
                ? fieldRepository
                        .findByDocumentTypeIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscLabelAsc(documentTypeId)
                : fieldRepository
                        .findByDocumentTypeIdAndDeletedFalseOrderBySortOrderAscLabelAsc(documentTypeId);
        return toResponses(fields);
    }

    @Override
    @Transactional(readOnly = true)
    public DmsMetadataFieldResponse findById(UUID id) {
        DmsMetadataField field = findEntity(id);
        return toResponse(field,
                optionRepository.findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(id));
    }

    @Override
    @Transactional
    public DmsMetadataFieldResponse create(
            UUID documentTypeId,
            DmsMetadataFieldCreateRequest request) {
        requireDocumentType(documentTypeId);
        String fieldKey = normalizeRequired(request.fieldKey());
        if (fieldRepository.existsByDocumentTypeIdAndFieldKeyIgnoreCaseAndDeletedFalse(
                documentTypeId, fieldKey)) {
            throw new DuplicateResourceException("Metadata field key already exists: " + fieldKey);
        }

        List<DmsMetadataField> existingFields = fieldRepository
                .findByDocumentTypeIdAndDeletedFalseOrderBySortOrderAscLabelAsc(documentTypeId);
        validateMasterSourceConfiguration(
                request.controlType(), request.masterSourceId(), request.parentFieldId(),
                request.sourceParameterName(), request.options());
        definitionValidator.validate(
                documentTypeId,
                fieldKey,
                request.controlType(),
                request.minLength(),
                request.maxLength(),
                request.minValue(),
                request.maxValue(),
                request.regexPattern(),
                defaultDateConstraint(request.dateConstraint()),
                request.visibilityCondition(),
                request.mandatoryCondition(),
                request.parentFieldId(),
                request.options(),
                existingFields);

        DmsMetadataField field = new DmsMetadataField();
        field.setDocumentTypeId(documentTypeId);
        field.setFieldKey(fieldKey);
        apply(field,
                request.label(),
                request.controlType(),
                request.placeholder(),
                request.helpText(),
                request.defaultValue(),
                request.sortOrder(),
                request.required(),
                request.readOnly(),
                request.hidden(),
                request.searchable(),
                request.active(),
                request.minLength(),
                request.maxLength(),
                request.minValue(),
                request.maxValue(),
                request.regexPattern(),
                request.dateConstraint(),
                request.visibilityCondition(),
                request.mandatoryCondition(),
                request.masterSourceId(),
                request.parentFieldId(),
                request.sourceParameterName(),
                existingFields);

        field = saveField(field);
        List<DmsMetadataOption> options = replaceOptions(field.getId(), request.options());
        auditTrailService.record(ENTITY_TYPE, field.getId(),
                "CREATE", null, auditValue(field, options.size()));
        return toResponse(field, options);
    }

    @Override
    @Transactional
    public DmsMetadataFieldResponse update(
            UUID id,
            DmsMetadataFieldUpdateRequest request) {
        DmsMetadataField field = findEntity(id);
        List<DmsMetadataField> existingFields = fieldRepository
                .findByDocumentTypeIdAndDeletedFalseOrderBySortOrderAscLabelAsc(field.getDocumentTypeId());

        validateMasterSourceConfiguration(
                request.controlType(), request.masterSourceId(), request.parentFieldId(),
                request.sourceParameterName(), request.options());
        definitionValidator.validate(
                field.getDocumentTypeId(),
                field.getFieldKey(),
                request.controlType(),
                request.minLength(),
                request.maxLength(),
                request.minValue(),
                request.maxValue(),
                request.regexPattern(),
                request.dateConstraint() == null
                        ? field.getDateConstraint()
                        : request.dateConstraint(),
                request.visibilityCondition(),
                request.mandatoryCondition(),
                request.parentFieldId(),
                request.options(),
                existingFields);

        List<DmsMetadataOption> oldOptions = optionRepository
                .findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(id);
        String oldValue = auditValue(field, oldOptions.size());
        if (Boolean.TRUE.equals(field.getActive()) && Boolean.FALSE.equals(request.active())) {
            ensureNotReferenced(field);
        }

        apply(field,
                request.label(),
                request.controlType(),
                request.placeholder(),
                request.helpText(),
                request.defaultValue(),
                request.sortOrder(),
                request.required(),
                request.readOnly(),
                request.hidden(),
                request.searchable(),
                request.active(),
                request.minLength(),
                request.maxLength(),
                request.minValue(),
                request.maxValue(),
                request.regexPattern(),
                request.dateConstraint(),
                request.visibilityCondition(),
                request.mandatoryCondition(),
                request.masterSourceId(),
                request.parentFieldId(),
                request.sourceParameterName(),
                existingFields);

        field = saveField(field);
        List<DmsMetadataOption> options = replaceOptions(id, request.options());
        auditTrailService.record(ENTITY_TYPE, id,
                "UPDATE", oldValue, auditValue(field, options.size()));
        return toResponse(field, options);
    }

    @Override
    @Transactional
    public DmsMetadataFieldResponse setActive(UUID id, boolean active) {
        DmsMetadataField field = findEntity(id);
        if (!active && Boolean.TRUE.equals(field.getActive())) {
            ensureNotReferenced(field);
        }
        List<DmsMetadataOption> options = optionRepository
                .findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(id);
        String oldValue = auditValue(field, options.size());
        field.setActive(active);
        field = fieldRepository.save(field);
        auditTrailService.record(ENTITY_TYPE, id,
                active ? "ACTIVATE" : "DEACTIVATE",
                oldValue,
                auditValue(field, options.size()));
        return toResponse(field, options);
    }

    @Override
    @Transactional
    public List<DmsMetadataFieldResponse> reorder(
            UUID documentTypeId,
            DmsMetadataFieldOrderRequest request) {
        requireDocumentType(documentTypeId);
        Set<UUID> requestedIds = new HashSet<>();
        for (DmsMetadataFieldOrderRequest.Item item : request.items()) {
            if (!requestedIds.add(item.id())) {
                throw new BusinessException("Duplicate metadata field in reorder request: " + item.id());
            }
        }

        List<DmsMetadataField> fields = fieldRepository.findByIdInAndDeletedFalse(requestedIds);
        if (fields.size() != requestedIds.size()
                || fields.stream().anyMatch(field -> !documentTypeId.equals(field.getDocumentTypeId()))) {
            throw new BusinessException("Reorder request contains a field outside the selected document type");
        }

        Map<UUID, Integer> orderById = request.items().stream()
                .collect(Collectors.toMap(
                        DmsMetadataFieldOrderRequest.Item::id,
                        DmsMetadataFieldOrderRequest.Item::sortOrder));
        for (DmsMetadataField field : fields) {
            Integer oldOrder = field.getSortOrder();
            field.setSortOrder(orderById.get(field.getId()));
            auditTrailService.record(ENTITY_TYPE, field.getId(),
                    "REORDER", String.valueOf(oldOrder), String.valueOf(field.getSortOrder()));
        }
        fieldRepository.saveAll(fields);
        return findAll(documentTypeId, false);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DmsMetadataField field = findEntity(id);
        ensureNotReferenced(field);

        List<DmsMetadataOption> options = optionRepository
                .findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(id);
        String oldValue = auditValue(field, options.size());
        field.setDeleted(Boolean.TRUE);
        fieldRepository.save(field);
        softDeleteOptions(options);
        auditTrailService.record(ENTITY_TYPE, id,
                "DELETE", oldValue, "soft-deleted");
    }


    private void validateMasterSourceConfiguration(
            DmsMetadataControlType controlType,
            UUID masterSourceId,
            UUID parentFieldId,
            String sourceParameterName,
            List<DmsMetadataOptionRequest> options) {
        String parameterName = normalizeOptional(sourceParameterName);
        if (masterSourceId == null) {
            if (parentFieldId != null || parameterName != null) {
                throw new BusinessException(
                        "Parent field and source parameter require a configured master source");
            }
            return;
        }
        if (!controlType.supportsOptions()) {
            throw new BusinessException(
                    "Master sources are supported only by option-based metadata controls");
        }
        if (options != null && !options.isEmpty()) {
            throw new BusinessException(
                    "A metadata field cannot use static options and a master source together");
        }
        var source = masterSourceRepository.findByIdAndDeletedFalse(masterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DMS master source", masterSourceId));
        if (!Boolean.TRUE.equals(source.getActive())) {
            throw new BusinessException("Selected DMS master source is inactive");
        }
        if (parentFieldId == null) {
            if (parameterName != null) {
                throw new BusinessException(
                        "Source parameter name requires a parent metadata field");
            }
            return;
        }
        if (parameterName == null) {
            throw new BusinessException(
                    "Source parameter name is required for a cascading metadata field");
        }
        boolean parameterExists = masterSourceParameterRepository
                .findByMasterSourceIdAndDeletedFalseOrderBySortOrderAscParameterNameAsc(masterSourceId)
                .stream()
                .anyMatch(parameter -> Boolean.TRUE.equals(parameter.getActive())
                        && parameter.getParameterName().equalsIgnoreCase(parameterName));
        if (!parameterExists) {
            throw new BusinessException(
                    "Source parameter does not exist in the selected master source: "
                            + parameterName);
        }
    }

    private void ensureNotReferenced(DmsMetadataField field) {
        UUID documentTypeId = field.getDocumentTypeId();
        String fieldKey = field.getFieldKey();
        if (fieldRepository
                .existsByDocumentTypeIdAndVisibilityConditionFieldKeyIgnoreCaseAndDeletedFalse(
                        documentTypeId, fieldKey)
                || fieldRepository
                .existsByDocumentTypeIdAndMandatoryConditionFieldKeyIgnoreCaseAndDeletedFalse(
                        documentTypeId, fieldKey)
                || fieldRepository.existsByParentFieldIdAndDeletedFalse(field.getId())) {
            throw new BusinessException(
                    "Metadata field is referenced by another field and cannot be deactivated or deleted");
        }
    }

    private void apply(
            DmsMetadataField field,
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
            DmsMetadataConditionRequest visibilityCondition,
            DmsMetadataConditionRequest mandatoryCondition,
            UUID masterSourceId,
            UUID parentFieldId,
            String sourceParameterName,
            List<DmsMetadataField> existingFields) {
        boolean existing = field.getId() != null;
        field.setLabel(normalizeRequired(label));
        field.setControlType(controlType);
        field.setPlaceholder(normalizeOptional(placeholder));
        field.setHelpText(normalizeOptional(helpText));
        field.setDefaultValue(normalizeOptional(defaultValue));
        field.setSortOrder(sortOrder == null
                ? (existing ? field.getSortOrder() : 0)
                : sortOrder);
        field.setRequired(resolveBoolean(required, field.getRequired(), false, existing));
        field.setReadOnly(resolveBoolean(readOnly, field.getReadOnly(), false, existing));
        field.setHidden(resolveBoolean(hidden, field.getHidden(), false, existing));
        field.setSearchable(resolveBoolean(searchable, field.getSearchable(), true, existing));
        field.setActive(resolveBoolean(active, field.getActive(), true, existing));
        field.setMinLength(minLength);
        field.setMaxLength(maxLength);
        field.setMinValue(minValue);
        field.setMaxValue(maxValue);
        field.setRegexPattern(normalizeOptional(regexPattern));
        field.setDateConstraint(dateConstraint == null
                ? (existing ? field.getDateConstraint() : DmsMetadataDateConstraint.NONE)
                : dateConstraint);
        applyCondition(field, visibilityCondition, true, existingFields);
        applyCondition(field, mandatoryCondition, false, existingFields);
        field.setMasterSourceId(masterSourceId);
        field.setParentFieldId(parentFieldId);
        field.setSourceParameterName(normalizeOptional(sourceParameterName));
    }

    private void applyCondition(
            DmsMetadataField field,
            DmsMetadataConditionRequest condition,
            boolean visibility,
            List<DmsMetadataField> existingFields) {
        String fieldKey = null;
        DmsMetadataConditionOperator operator = null;
        String value = null;
        if (condition != null) {
            fieldKey = canonicalFieldKey(condition.fieldKey(), existingFields);
            operator = condition.operator();
            value = operator.requiresValue() ? normalizeOptional(condition.value()) : null;
        }

        if (visibility) {
            field.setVisibilityConditionFieldKey(fieldKey);
            field.setVisibilityConditionOperator(operator);
            field.setVisibilityConditionValue(value);
        } else {
            field.setMandatoryConditionFieldKey(fieldKey);
            field.setMandatoryConditionOperator(operator);
            field.setMandatoryConditionValue(value);
        }
    }

    private String canonicalFieldKey(String requestedKey, List<DmsMetadataField> existingFields) {
        return existingFields.stream()
                .filter(existing -> existing.getFieldKey().equalsIgnoreCase(requestedKey.trim()))
                .map(DmsMetadataField::getFieldKey)
                .findFirst()
                .orElse(requestedKey.trim());
    }

    private List<DmsMetadataOption> replaceOptions(
            UUID metadataFieldId,
            List<DmsMetadataOptionRequest> requests) {
        List<DmsMetadataOption> current = optionRepository
                .findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(metadataFieldId);
        softDeleteOptions(current);

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<DmsMetadataOption> replacements = new ArrayList<>();
        int fallbackOrder = 0;
        for (DmsMetadataOptionRequest request : requests) {
            DmsMetadataOption option = new DmsMetadataOption();
            option.setMetadataFieldId(metadataFieldId);
            option.setValue(normalizeRequired(request.value()));
            option.setLabel(normalizeRequired(request.label()));
            option.setSortOrder(request.sortOrder() == null ? fallbackOrder : request.sortOrder());
            option.setActive(defaultBoolean(request.active(), true));
            replacements.add(option);
            fallbackOrder += 10;
        }
        return optionRepository.saveAll(replacements).stream()
                .sorted(Comparator.comparing(DmsMetadataOption::getSortOrder)
                        .thenComparing(DmsMetadataOption::getLabel))
                .toList();
    }

    private void softDeleteOptions(List<DmsMetadataOption> options) {
        if (options.isEmpty()) {
            return;
        }
        options.forEach(option -> option.setDeleted(Boolean.TRUE));
        optionRepository.saveAll(options);
        optionRepository.flush();
    }

    private List<DmsMetadataFieldResponse> toResponses(List<DmsMetadataField> fields) {
        if (fields.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = fields.stream().map(DmsMetadataField::getId).toList();
        Map<UUID, List<DmsMetadataOption>> optionsByField = optionRepository
                .findByMetadataFieldIdInAndDeletedFalseOrderBySortOrderAscLabelAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(DmsMetadataOption::getMetadataFieldId));
        return fields.stream()
                .map(field -> toResponse(field,
                        optionsByField.getOrDefault(field.getId(), List.of())))
                .toList();
    }

    private DmsMetadataFieldResponse toResponse(
            DmsMetadataField field,
            List<DmsMetadataOption> options) {
        return new DmsMetadataFieldResponse(
                field.getId(),
                field.getDocumentTypeId(),
                field.getFieldKey(),
                field.getLabel(),
                field.getControlType(),
                field.getPlaceholder(),
                field.getHelpText(),
                field.getDefaultValue(),
                field.getSortOrder(),
                field.getRequired(),
                field.getReadOnly(),
                field.getHidden(),
                field.getSearchable(),
                field.getActive(),
                field.getMinLength(),
                field.getMaxLength(),
                field.getMinValue(),
                field.getMaxValue(),
                field.getRegexPattern(),
                field.getDateConstraint(),
                condition(field.getVisibilityConditionFieldKey(),
                        field.getVisibilityConditionOperator(),
                        field.getVisibilityConditionValue()),
                condition(field.getMandatoryConditionFieldKey(),
                        field.getMandatoryConditionOperator(),
                        field.getMandatoryConditionValue()),
                field.getMasterSourceId(),
                field.getParentFieldId(),
                field.getSourceParameterName(),
                options.stream()
                        .map(option -> new DmsMetadataFieldResponse.Option(
                                option.getId(),
                                option.getValue(),
                                option.getLabel(),
                                option.getSortOrder(),
                                option.getActive()))
                        .toList(),
                field.getCreatedAt(),
                field.getUpdatedAt());
    }

    private DmsMetadataFieldResponse.Condition condition(
            String fieldKey,
            DmsMetadataConditionOperator operator,
            String value) {
        if (fieldKey == null || operator == null) {
            return null;
        }
        return new DmsMetadataFieldResponse.Condition(fieldKey, operator, value);
    }

    private DmsMetadataField saveField(DmsMetadataField field) {
        try {
            return fieldRepository.saveAndFlush(field);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "A metadata field with the same key already exists in this document type");
        }
    }

    private DmsMetadataField findEntity(UUID id) {
        return fieldRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DMS metadata field", id));
    }

    private void requireDocumentType(UUID documentTypeId) {
        documentTypeRepository.findByIdAndDeletedFalse(documentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document type", documentTypeId));
    }

    private DmsMetadataDateConstraint defaultDateConstraint(
            DmsMetadataDateConstraint dateConstraint) {
        return dateConstraint == null ? DmsMetadataDateConstraint.NONE : dateConstraint;
    }

    private boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean resolveBoolean(
            Boolean requested,
            Boolean current,
            boolean defaultValue,
            boolean existing) {
        if (requested != null) {
            return requested;
        }
        if (existing && current != null) {
            return current;
        }
        return defaultValue;
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String auditValue(DmsMetadataField field, int optionCount) {
        return "fieldKey=" + field.getFieldKey()
                + ", label=" + field.getLabel()
                + ", controlType=" + field.getControlType()
                + ", required=" + field.getRequired()
                + ", active=" + field.getActive()
                + ", sortOrder=" + field.getSortOrder()
                + ", options=" + optionCount;
    }
}
