package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.dms.dto.DmsDynamicFormOptionsResponse;
import com.bor.eboard.dms.dto.DmsDynamicFormResponse;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsDocumentType;
import com.bor.eboard.dms.entity.DmsMetadataField;
import com.bor.eboard.dms.entity.DmsMetadataOption;
import com.bor.eboard.dms.repository.DmsDocumentTypeRepository;
import com.bor.eboard.dms.repository.DmsMetadataFieldRepository;
import com.bor.eboard.dms.repository.DmsMetadataOptionRepository;
import com.bor.eboard.dms.service.DmsDynamicFormService;
import com.bor.eboard.dms.service.DmsMasterSourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DmsDynamicFormServiceImpl implements DmsDynamicFormService {

    private final DmsDocumentTypeRepository documentTypeRepository;
    private final DmsMetadataFieldRepository fieldRepository;
    private final DmsMetadataOptionRepository optionRepository;
    private final DmsMasterSourceService masterSourceService;

    public DmsDynamicFormServiceImpl(
            DmsDocumentTypeRepository documentTypeRepository,
            DmsMetadataFieldRepository fieldRepository,
            DmsMetadataOptionRepository optionRepository,
            DmsMasterSourceService masterSourceService) {
        this.documentTypeRepository = documentTypeRepository;
        this.fieldRepository = fieldRepository;
        this.optionRepository = optionRepository;
        this.masterSourceService = masterSourceService;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDynamicFormResponse getForm(UUID documentTypeId) {
        DmsDocumentType documentType = requireActiveDocumentType(documentTypeId);
        List<DmsMetadataField> fields = activeFields(documentTypeId);
        Map<UUID, String> fieldKeysById = fields.stream()
                .collect(Collectors.toMap(DmsMetadataField::getId, DmsMetadataField::getFieldKey));
        Map<UUID, List<DmsMetadataOption>> optionsByField = loadStaticOptions(fields);

        List<DmsDynamicFormResponse.Field> definitions = fields.stream()
                .map(field -> toDefinition(
                        field,
                        fieldKeysById.get(field.getParentFieldId()),
                        optionsByField.getOrDefault(field.getId(), List.of())))
                .toList();

        return new DmsDynamicFormResponse(
                documentType.getId(),
                documentType.getCode(),
                documentType.getName(),
                documentType.getDescription(),
                definitions);
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDynamicFormOptionsResponse resolveOptions(
            UUID documentTypeId,
            String fieldKey,
            Map<String, Object> values) {
        requireActiveDocumentType(documentTypeId);
        DmsMetadataField field = fieldRepository
                .findByDocumentTypeIdAndFieldKeyIgnoreCaseAndActiveTrueAndDeletedFalse(
                        documentTypeId, fieldKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active DMS metadata field not found: " + fieldKey));

        if (!field.getControlType().supportsOptions()) {
            throw new BusinessException(
                    "Metadata field does not support selectable options: " + field.getFieldKey());
        }

        List<DmsDynamicFormResponse.Option> options;
        if (field.getMasterSourceId() == null) {
            options = staticOptions(field.getId());
        } else {
            Map<String, Object> parameters = resolveParameters(field, values);
            if (field.getParentFieldId() != null && parameters.isEmpty()) {
                options = List.of();
            } else {
                options = masterSourceService.resolve(field.getMasterSourceId(), parameters).stream()
                        .map(this::toOption)
                        .toList();
            }
        }
        return new DmsDynamicFormOptionsResponse(field.getFieldKey(), options);
    }

    private Map<String, Object> resolveParameters(
            DmsMetadataField field,
            Map<String, Object> values) {
        if (field.getParentFieldId() == null) {
            return Map.of();
        }
        DmsMetadataField parent = fieldRepository.findByIdAndDeletedFalse(field.getParentFieldId())
                .filter(item -> item.getDocumentTypeId().equals(field.getDocumentTypeId()))
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new BusinessException(
                        "Configured parent metadata field is unavailable"));

        Object parentValue = findValue(values, parent.getFieldKey());
        if (isEmpty(parentValue)) {
            return Map.of();
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(field.getSourceParameterName(), parentValue);
        return parameters;
    }

    private DmsDynamicFormResponse.Field toDefinition(
            DmsMetadataField field,
            String parentFieldKey,
            List<DmsMetadataOption> options) {
        return new DmsDynamicFormResponse.Field(
                field.getId(),
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
                field.getMinLength(),
                field.getMaxLength(),
                field.getMinValue(),
                field.getMaxValue(),
                field.getRegexPattern(),
                field.getDateConstraint(),
                condition(
                        field.getVisibilityConditionFieldKey(),
                        field.getVisibilityConditionOperator(),
                        field.getVisibilityConditionValue()),
                condition(
                        field.getMandatoryConditionFieldKey(),
                        field.getMandatoryConditionOperator(),
                        field.getMandatoryConditionValue()),
                field.getMasterSourceId(),
                parentFieldKey,
                field.getSourceParameterName(),
                options.stream()
                        .filter(option -> Boolean.TRUE.equals(option.getActive()))
                        .map(option -> new DmsDynamicFormResponse.Option(
                                option.getValue(), option.getLabel()))
                        .toList());
    }

    private DmsDynamicFormResponse.Condition condition(
            String fieldKey,
            com.bor.eboard.dms.metadata.DmsMetadataConditionOperator operator,
            String value) {
        if (fieldKey == null || operator == null) {
            return null;
        }
        return new DmsDynamicFormResponse.Condition(fieldKey, operator, value);
    }

    private Map<UUID, List<DmsMetadataOption>> loadStaticOptions(
            Collection<DmsMetadataField> fields) {
        List<UUID> ids = fields.stream()
                .filter(field -> field.getMasterSourceId() == null)
                .filter(field -> field.getControlType().supportsOptions())
                .map(DmsMetadataField::getId)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return optionRepository
                .findByMetadataFieldIdInAndDeletedFalseOrderBySortOrderAscLabelAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        DmsMetadataOption::getMetadataFieldId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private List<DmsDynamicFormResponse.Option> staticOptions(UUID fieldId) {
        return optionRepository
                .findByMetadataFieldIdAndDeletedFalseOrderBySortOrderAscLabelAsc(fieldId)
                .stream()
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .map(option -> new DmsDynamicFormResponse.Option(
                        option.getValue(), option.getLabel()))
                .toList();
    }

    private DmsDynamicFormResponse.Option toOption(DmsMasterDataOptionResponse option) {
        return new DmsDynamicFormResponse.Option(option.value(), option.label());
    }

    private List<DmsMetadataField> activeFields(UUID documentTypeId) {
        return fieldRepository
                .findByDocumentTypeIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscLabelAsc(
                        documentTypeId);
    }

    private DmsDocumentType requireActiveDocumentType(UUID documentTypeId) {
        DmsDocumentType documentType = documentTypeRepository.findByIdAndDeletedFalse(documentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DMS document type", documentTypeId));
        if (!Boolean.TRUE.equals(documentType.getActive())) {
            throw new BusinessException("DMS document type is inactive");
        }
        return documentType;
    }

    private Object findValue(Map<String, Object> values, String fieldKey) {
        if (values == null || values.isEmpty()) {
            return null;
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
        if (value instanceof CharSequence text) {
            return text.toString().trim().isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }
}
