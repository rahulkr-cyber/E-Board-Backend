package com.bor.eboard.checklist.service.impl;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.repository.AuditLogRepository;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.checklist.dto.ChecklistDtos;
import com.bor.eboard.checklist.entity.ChecklistInstance;
import com.bor.eboard.checklist.entity.ChecklistInstanceItem;
import com.bor.eboard.checklist.entity.ChecklistItem;
import com.bor.eboard.checklist.entity.ChecklistTemplate;
import com.bor.eboard.checklist.entity.LetterCategoryChecklist;
import com.bor.eboard.checklist.repository.ChecklistInstanceItemRepository;
import com.bor.eboard.checklist.repository.ChecklistInstanceRepository;
import com.bor.eboard.checklist.repository.ChecklistItemRepository;
import com.bor.eboard.checklist.repository.ChecklistTemplateRepository;
import com.bor.eboard.checklist.repository.LetterCategoryChecklistRepository;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.entity.Attachment;
import com.bor.eboard.correspondence.entity.Letter;
import com.bor.eboard.correspondence.repository.AttachmentRepository;
import com.bor.eboard.correspondence.repository.LetterRepository;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.registry.entity.DiaryEntry;
import com.bor.eboard.registry.repository.DiaryEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistServiceImpl implements ChecklistService {

    private static final String MODULE = "CHECKLIST";
    private static final String ENTITY_INSTANCE = "CHECKLIST_INSTANCE";
    private static final String DOCUMENT_UPLOAD = "DOCUMENT_UPLOAD";
    private static final Set<String> CONTROL_TYPES = Set.of(
            DOCUMENT_UPLOAD, "YES_NO", "CHECKBOX", "TEXTBOX", "TEXT_AREA", "NUMBER",
            "DATE", "DATE_TIME", "DROPDOWN", "MULTI_SELECT", "RADIO_BUTTON", "LABEL",
            "REMARKS", "AUTO_VALUE", "HYPERLINK", "SIGNATURE");
    private static final Set<String> CONDITION_OPERATORS = Set.of(
            "EQUALS", "NOT_EQUALS", "TRUE", "FALSE", "IS_EMPTY", "IS_NOT_EMPTY",
            "CONTAINS", "IN", "GREATER_THAN", "LESS_THAN");
    private static final Set<String> VALUE_CONDITION_OPERATORS = Set.of(
            "EQUALS", "NOT_EQUALS", "CONTAINS", "IN", "GREATER_THAN", "LESS_THAN");
    private static final Set<String> VALID_ITEM_STATUSES =
            Set.of("PENDING", "ANSWERED", "INFORMATION", "UPLOADED", "VERIFIED", "REJECTED", "NOT_APPLICABLE");
    private static final Set<String> FORWARD_READY =
            Set.of("ANSWERED", "UPLOADED", "VERIFIED", "NOT_APPLICABLE", "INFORMATION");

    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistItemRepository itemRepository;
    private final LetterCategoryChecklistRepository mappingRepository;
    private final ChecklistInstanceRepository instanceRepository;
    private final ChecklistInstanceItemRepository instanceItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final MasterDataFacade masterDataFacade;
    private final IdentityFacade identityFacade;
    private final DiaryEntryRepository diaryRepository;
    private final LetterRepository letterRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistDtos.TemplateResponse> getTemplates() {
        return templateRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistDtos.TemplateResponse getTemplate(UUID id) {
        return toTemplateResponse(findTemplate(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChecklistDtos.TemplateResponse> getTemplateForCategory(UUID categoryId) {
        return mappedTemplate(categoryId).map(this::toTemplateResponse);
    }

    @Override
    @Transactional
    public ChecklistDtos.TemplateResponse createTemplate(ChecklistDtos.TemplateRequest request) {
        requireAuthority("CHECKLIST_ADMIN");
        String code = normalizeCode(request.getCode());
        if (templateRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Checklist template code already exists: " + code);
        }
        ChecklistTemplate template = new ChecklistTemplate();
        applyTemplate(template, request, code);
        template = templateRepository.save(template);
        saveTemplateItems(template.getId(), request.getItems());
        auditService.record(MODULE, "CHECKLIST_TEMPLATE", template.getId(),
                "CREATE", null, template.getName());
        return toTemplateResponse(template);
    }

    @Override
    @Transactional
    public ChecklistDtos.TemplateResponse updateTemplate(UUID id, ChecklistDtos.TemplateRequest request) {
        requireAuthority("CHECKLIST_ADMIN");
        ChecklistTemplate template = findTemplate(id);
        String code = normalizeCode(request.getCode());
        templateRepository.findByCodeAndDeletedFalse(code)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Checklist template code already exists: " + code);
                });
        String old = template.getName();
        applyTemplate(template, request, code);
        templateRepository.save(template);

        List<ChecklistItem> existing = itemRepository
                .findByChecklistTemplateIdAndDeletedFalseOrderBySequenceAsc(id);
        existing.forEach(item -> item.setDeleted(Boolean.TRUE));
        itemRepository.saveAll(existing);
        saveTemplateItems(id, request.getItems());

        auditService.record(MODULE, "CHECKLIST_TEMPLATE", id,
                "UPDATE", old, template.getName());
        return toTemplateResponse(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        requireAuthority("CHECKLIST_ADMIN");
        ChecklistTemplate template = findTemplate(id);
        if (instanceRepository.existsByChecklistTemplateIdAndDeletedFalse(id)) {
            throw new BusinessException("Checklist template is already used by a diary/letter and cannot be deleted");
        }
        template.setDeleted(Boolean.TRUE);
        templateRepository.save(template);
        mappingRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(m -> id.equals(m.getChecklistTemplateId()))
                .forEach(m -> {
                    m.setDeleted(Boolean.TRUE);
                    mappingRepository.save(m);
                });
        auditService.record(MODULE, "CHECKLIST_TEMPLATE", id, "DELETE", template.getName(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistDtos.MappingResponse> getMappings() {
        Map<UUID, String> categories = masterDataFacade.categoryNames();
        Map<UUID, String> templates = templateRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(ChecklistTemplate::getId, ChecklistTemplate::getName));
        return mappingRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(m -> ChecklistDtos.MappingResponse.builder()
                        .id(m.getId())
                        .categoryId(m.getCategoryId())
                        .categoryName(categories.get(m.getCategoryId()))
                        .templateId(m.getChecklistTemplateId())
                        .templateName(templates.get(m.getChecklistTemplateId()))
                        .active(m.getActive())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ChecklistDtos.MappingResponse saveMapping(ChecklistDtos.MappingRequest request) {
        requireAuthority("CHECKLIST_ADMIN");
        if (!masterDataFacade.categoryExists(request.getCategoryId())) {
            throw new ValidationException("Invalid letter category");
        }
        ChecklistTemplate template = findTemplate(request.getTemplateId());
        if (!Boolean.TRUE.equals(template.getActive())) {
            throw new ValidationException("Inactive checklist template cannot be mapped");
        }
        LetterCategoryChecklist mapping = mappingRepository
                .findByCategoryIdAndDeletedFalse(request.getCategoryId())
                .orElseGet(LetterCategoryChecklist::new);
        mapping.setCategoryId(request.getCategoryId());
        mapping.setChecklistTemplateId(template.getId());
        mapping.setActive(request.getActive() == null || request.getActive());
        mapping = mappingRepository.save(mapping);
        auditService.record(MODULE, "CATEGORY_CHECKLIST", mapping.getId(),
                "MAP", null, "categoryId=" + request.getCategoryId() + ", templateId=" + template.getId());
        Map<UUID, String> categories = masterDataFacade.categoryNames();
        return ChecklistDtos.MappingResponse.builder()
                .id(mapping.getId()).categoryId(mapping.getCategoryId())
                .categoryName(categories.get(mapping.getCategoryId()))
                .templateId(template.getId()).templateName(template.getName())
                .active(mapping.getActive()).build();
    }

    @Override
    @Transactional
    public void deleteMapping(UUID categoryId) {
        requireAuthority("CHECKLIST_ADMIN");
        LetterCategoryChecklist mapping = mappingRepository.findByCategoryIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist category mapping", categoryId));
        mapping.setDeleted(Boolean.TRUE);
        mappingRepository.save(mapping);
        auditService.record(MODULE, "CATEGORY_CHECKLIST", mapping.getId(),
                "UNMAP", "categoryId=" + categoryId, null);
    }

    @Override
    @Transactional
    public void syncForDiary(UUID diaryId, UUID categoryId) {
        Optional<ChecklistTemplate> mapped = mappedTemplate(categoryId);
        Optional<ChecklistInstance> existing = instanceRepository.findByDiaryEntryIdAndDeletedFalse(diaryId);

        if (mapped.isEmpty()) {
            existing.ifPresent(this::deleteUnusedInstance);
            return;
        }
        ChecklistTemplate template = mapped.get();
        if (existing.isPresent() && template.getId().equals(existing.get().getChecklistTemplateId())) {
            ChecklistInstance instance = existing.get();
            if (!Objects.equals(categoryId, instance.getCategoryId())) {
                instance.setCategoryId(categoryId);
                instanceRepository.save(instance);
            }
            return;
        }
        existing.ifPresent(this::deleteUnusedInstance);

        ChecklistInstance instance = new ChecklistInstance();
        instance.setChecklistTemplateId(template.getId());
        instance.setCategoryId(categoryId);
        instance.setDiaryEntryId(diaryId);
        instance.setStatus("INCOMPLETE");
        instance = instanceRepository.save(instance);
        ChecklistInstance savedInstance = instance;

        List<ChecklistInstanceItem> snapshots = itemRepository
                .findByChecklistTemplateIdAndActiveTrueAndDeletedFalseOrderBySequenceAsc(template.getId())
                .stream().map(item -> snapshot(savedInstance, item)).toList();
        instanceItemRepository.saveAll(snapshots);
        refreshStatus(savedInstance);
        recordInstanceEvent(savedInstance.getId(), "CREATED", null,
                template.getName() + " loaded automatically");
    }

    @Override
    @Transactional
    public void linkDiaryToLetter(UUID diaryId, UUID letterId) {
        instanceRepository.findByDiaryEntryIdAndDeletedFalse(diaryId).ifPresent(instance -> {
            instance.setLetterId(letterId);
            instanceRepository.save(instance);
            recordInstanceEvent(instance.getId(), "LINKED_TO_LETTER", null,
                    "Checklist linked to generated letter " + letterId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChecklistDtos.InstanceResponse> getForDiary(UUID diaryId) {
        return instanceRepository.findByDiaryEntryIdAndDeletedFalse(diaryId).map(this::toInstanceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChecklistDtos.InstanceResponse> getForLetter(UUID letterId) {
        return instanceRepository.findByLetterIdAndDeletedFalse(letterId).map(this::toInstanceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChecklistDtos.Summary> getSummaryForDiary(UUID diaryId) {
        return instanceRepository.findByDiaryEntryIdAndDeletedFalse(diaryId).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChecklistDtos.Summary> getSummaryForLetter(UUID letterId) {
        return instanceRepository.findByLetterIdAndDeletedFalse(letterId).map(this::toSummary);
    }

    @Override
    @Transactional
    public ChecklistDtos.InstanceResponse upload(UUID instanceItemId, MultipartFile file, String remarks) {
        requireAuthority("CHECKLIST_UPLOAD");
        ChecklistInstanceItem item = findInstanceItem(instanceItemId);
        if (!isDocument(item)) {
            throw new ValidationException("This checklist item expects a " + item.getControlType() + " response, not a document");
        }
        ChecklistInstance instance = findInstance(item.getChecklistInstanceId());
        if (Boolean.TRUE.equals(item.getRemarksRequired()) && !StringUtils.hasText(remarks)) {
            throw new ValidationException("Remarks are required for " + label(item));
        }
        validateFile(item, file);
        boolean replacingRejected = "REJECTED".equals(item.getStatus());
        if (!Boolean.TRUE.equals(item.getAllowMultipleAttachments()) && !replacingRejected
                && attachmentRepository.countByChecklistInstanceItemIdAndActiveTrueAndDeletedFalse(item.getId()) > 0) {
            throw new ValidationException("Only one attachment is allowed for " + label(item));
        }
        String linkedType = instance.getLetterId() != null ? "LETTER" : "DIARY";
        UUID linkedId = instance.getLetterId() != null ? instance.getLetterId() : instance.getDiaryEntryId();
        attachmentStorageService.store(linkedType, linkedId, null, item.getId(), file);

        UUID userId = currentUserId();
        item.setStatus("UPLOADED");
        item.setUploadedBy(userId);
        item.setUploadedAt(LocalDateTime.now());
        item.setVerifiedBy(null);
        item.setVerifiedAt(null);
        if (StringUtils.hasText(remarks)) {
            item.setRemarks(remarks.trim());
        } else if (replacingRejected) {
            item.setRemarks(null);
        }
        instanceItemRepository.save(item);
        refreshStatus(instance);
        recordInstanceEvent(instance.getId(), "UPLOADED", item,
                file.getOriginalFilename() + " uploaded");
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public ChecklistDtos.InstanceResponse saveResponse(UUID instanceItemId,
                                                        ChecklistDtos.ResponseRequest request) {
        requireAuthority("CHECKLIST_UPLOAD");
        ChecklistInstanceItem item = findInstanceItem(instanceItemId);
        ChecklistInstance instance = findInstance(item.getChecklistInstanceId());
        if (isDocument(item)) {
            throw new ValidationException("Use the attachment API for " + label(item));
        }
        if (Set.of("LABEL", "AUTO_VALUE", "SIGNATURE").contains(item.getControlType())
                || Boolean.TRUE.equals(item.getReadOnly())) {
            throw new ValidationException(label(item) + " is read only");
        }
        List<ChecklistInstanceItem> items = instanceItemRepository
                .findByChecklistInstanceIdAndActiveTrueAndDeletedFalseOrderBySequenceAsc(instance.getId());
        Map<Integer, ChecklistInstanceItem> bySequence = items.stream()
                .collect(Collectors.toMap(ChecklistInstanceItem::getSequence, Function.identity()));
        if (!effectiveVisible(item, bySequence)) {
            throw new ValidationException(label(item) + " is not currently visible");
        }
        validateResponse(item, request.getValue(), effectiveMandatory(item, bySequence));
        if (Boolean.TRUE.equals(item.getRemarksRequired()) && !StringUtils.hasText(request.getRemarks())) {
            throw new ValidationException("Remarks are required for " + label(item));
        }

        UUID userId = currentUserId();
        item.setResponseValueJson(encode(request.getValue()));
        item.setResponseDisplayValue(displayValue(request.getValue()));
        item.setRespondedBy(userId);
        item.setRespondedAt(LocalDateTime.now());
        item.setStatus(isEmptyValue(request.getValue()) ? "PENDING" : "ANSWERED");
        item.setVerifiedBy(null);
        item.setVerifiedAt(null);
        if (StringUtils.hasText(request.getRemarks())) {
            item.setRemarks(request.getRemarks().trim());
        }
        instanceItemRepository.save(item);
        refreshStatus(instance);
        recordInstanceEvent(instance.getId(), "RESPONSE_SAVED", item,
                label(item) + " = " + fallback(item.getResponseDisplayValue()));
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public ChecklistDtos.InstanceResponse verify(UUID instanceItemId,
                                                  ChecklistDtos.VerificationRequest request) {
        requireAuthority("CHECKLIST_VERIFY");
        ChecklistInstanceItem item = findInstanceItem(instanceItemId);
        ChecklistInstance instance = findInstance(item.getChecklistInstanceId());
        String status = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!VALID_ITEM_STATUSES.contains(status) || Set.of("UPLOADED", "ANSWERED", "INFORMATION").contains(status)) {
            throw new ValidationException("Invalid verification status");
        }
        if ("VERIFIED".equals(status)) {
            if (isDocument(item)
                    && attachmentRepository.countByChecklistInstanceItemIdAndActiveTrueAndDeletedFalse(item.getId()) == 0) {
                throw new ValidationException("Upload a document before verifying " + label(item));
            }
            if (!isDocument(item) && !responseComplete(item)) {
                throw new ValidationException("Save a response before verifying " + label(item));
            }
        }
        if (("REJECTED".equals(status) || Boolean.TRUE.equals(item.getRemarksRequired()))
                && !StringUtils.hasText(request.getRemarks())) {
            throw new ValidationException("Remarks are required");
        }
        UUID userId = currentUserId();
        item.setStatus(status);
        item.setRemarks(StringUtils.hasText(request.getRemarks()) ? request.getRemarks().trim() : item.getRemarks());
        if (Set.of("VERIFIED", "REJECTED", "NOT_APPLICABLE").contains(status)) {
            item.setVerifiedBy(userId);
            item.setVerifiedAt(LocalDateTime.now());
        } else {
            item.setVerifiedBy(null);
            item.setVerifiedAt(null);
        }
        instanceItemRepository.save(item);
        refreshStatus(instance);
        recordInstanceEvent(instance.getId(), status, item,
                item.getRemarks() != null ? item.getRemarks() : label(item));
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public ChecklistDtos.InstanceResponse requestAdditionalItem(
            UUID instanceId, ChecklistDtos.AdditionalItemRequest request) {
        requireAuthority("CHECKLIST_VERIFY");
        ChecklistInstance instance = findInstance(instanceId);
        List<ChecklistInstanceItem> current = instanceItemRepository
                .findByChecklistInstanceIdAndDeletedFalseOrderBySequenceAsc(instanceId);
        int nextSequence = current.stream().map(ChecklistInstanceItem::getSequence)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
        ChecklistInstanceItem item = new ChecklistInstanceItem();
        item.setChecklistInstanceId(instanceId);
        item.setItemName(request.getName().trim());
        item.setDisplayLabel(blankToNull(request.getLabel()));
        item.setDescription(blankToNull(request.getDescription()));
        item.setControlType(normalizeControlType(request.getControlType()));
        item.setPlaceholder(blankToNull(request.getPlaceholder()));
        item.setHelpText(blankToNull(request.getHelpText()));
        item.setMandatory(Boolean.TRUE.equals(request.getMandatory()));
        item.setReadOnly(Boolean.TRUE.equals(request.getReadOnly()));
        item.setVisible(request.getVisible() == null || request.getVisible());
        item.setDefaultValueJson(encode(request.getDefaultValue()));
        item.setValidationRulesJson(encode(request.getValidationRules()));
        item.setMinimumLength(request.getMinimumLength());
        item.setMaximumLength(request.getMaximumLength());
        item.setRegexPattern(blankToNull(request.getRegexPattern()));
        item.setOptionsJson(encode(cleanOptions(request.getOptions())));
        item.setAutoValueExpression(blankToNull(request.getAutoValueExpression()));
        item.setParentItemSequence(request.getParentItemSequence());
        item.setVisibilityConditionOperator(normalizeOperator(request.getVisibilityConditionOperator()));
        item.setVisibilityConditionValue(encode(request.getVisibilityConditionValue()));
        item.setMandatoryConditionOperator(normalizeOperator(request.getMandatoryConditionOperator()));
        item.setMandatoryConditionValue(encode(request.getMandatoryConditionValue()));
        item.setSequence(nextSequence);
        item.setRemarksRequired(Boolean.TRUE.equals(request.getRemarksRequired()));
        item.setAllowMultipleAttachments(Boolean.TRUE.equals(request.getAllowMultipleAttachments()));
        item.setSupportedFileTypes(normalizeTypes(request.getSupportedFileTypes()));
        item.setMaximumFileSizeBytes(request.getMaximumFileSizeBytes());
        item.setRequestedBy(currentUserId());
        item.setRequestedAt(LocalDateTime.now());
        item.setRemarks(blankToNull(request.getRemarks()));
        item.setActive(Boolean.TRUE);
        validateItemMetadata(item, current.stream().map(ChecklistInstanceItem::getSequence).collect(Collectors.toSet()));
        applyInitialValue(instance, item);
        item = instanceItemRepository.save(item);
        refreshStatus(instance);
        recordInstanceEvent(instance.getId(), "REQUESTED", item,
                "Additional checklist item requested: " + label(item));
        return toInstanceResponse(instance);
    }

    @Override
    @Transactional
    public void validateDiaryForward(UUID diaryId, boolean override, String overrideRemarks) {
        instanceRepository.findByDiaryEntryIdAndDeletedFalse(diaryId)
                .ifPresent(instance -> validateForward(instance, override, overrideRemarks));
    }

    @Override
    @Transactional
    public void validateLetterForward(UUID letterId, boolean override, String overrideRemarks) {
        instanceRepository.findByLetterIdAndDeletedFalse(letterId)
                .ifPresent(instance -> validateForward(instance, override, overrideRemarks));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> matchingDiaryIds(UUID templateId, String status, Boolean complete,
                                       Boolean missingMandatory) {
        return matchingInstances(templateId, status, complete, missingMandatory).stream()
                .map(ChecklistInstance::getDiaryEntryId).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> matchingLetterIds(UUID templateId, String status, Boolean complete,
                                        Boolean missingMandatory) {
        return matchingInstances(templateId, status, complete, missingMandatory).stream()
                .map(ChecklistInstance::getLetterId).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistDtos.DashboardSummary dashboard(UUID ownerId, List<UUID> sectionIds,
                                                     boolean organizationWide) {
        List<ChecklistInstance> instances;
        if (organizationWide) {
            instances = instanceRepository.findByDeletedFalseOrderByCreatedAtDesc();
        } else if (ownerId != null) {
            instances = instanceRepository.findForOwner(ownerId);
        } else if (sectionIds != null && !sectionIds.isEmpty()) {
            instances = instanceRepository.findForSections(sectionIds);
        } else {
            instances = List.of();
        }
        Map<UUID, String> categories = masterDataFacade.categoryNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> departments = identityFacade.departmentNames();

        List<SummaryRow> rows = instances.stream().map(instance -> {
            ChecklistDtos.Summary summary = toSummary(instance);
            UUID sectionId = resolveSectionId(instance);
            UUID departmentId = sectionId == null ? null
                    : identityFacade.findSection(sectionId).map(IdentityFacade.SectionRef::departmentId).orElse(null);
            return new SummaryRow(instance, summary, categories.get(instance.getCategoryId()),
                    sections.get(sectionId), departments.get(departmentId));
        }).toList();

        long total = rows.size();
        long completeCount = rows.stream().filter(r -> r.summary().isComplete()).count();
        long incompleteCount = rows.stream().filter(r -> !r.summary().isComplete()).count();
        long exceptions = rows.stream().filter(r -> r.summary().isForwardOverrideUsed()).count();
        long pending = rows.stream().filter(r -> r.summary().getPendingVerification() > 0).count();
        long pendingResponses = rows.stream().mapToLong(r -> r.summary().getPendingResponse()).sum();
        long totalResponses = rows.stream().mapToLong(r -> r.summary().getTotalItems()).sum();
        long answeredResponses = rows.stream().mapToLong(r -> r.summary().getAnsweredItems() + r.summary().getUploadedItems()
                + r.summary().getCompletedItems()).sum();
        long missing = rows.stream().filter(r -> r.instance().getLetterId() != null
                && r.summary().getMissingMandatory() > 0).count();

        return ChecklistDtos.DashboardSummary.builder()
                .totalChecklists(total)
                .complete(completeCount)
                .incomplete(incompleteCount)
                .forwardedWithExceptions(exceptions)
                .pendingVerification(pending)
                .pendingResponses(pendingResponses)
                .totalResponses(totalResponses)
                .answeredResponses(answeredResponses)
                .lettersWithMissingDocuments(missing)
                .compliancePercent(percent(completeCount, total))
                .incompleteByCategory(rows.stream().filter(r -> !r.summary().isComplete())
                        .collect(Collectors.groupingBy(r -> fallback(r.categoryName()), LinkedHashMap::new,
                                Collectors.counting())))
                .complianceByDepartment(compliance(rows, SummaryRow::departmentName))
                .complianceBySection(compliance(rows, SummaryRow::sectionName))
                .complianceByCategory(compliance(rows, SummaryRow::categoryName))
                .build();
    }

    private void validateForward(ChecklistInstance instance, boolean override, String overrideRemarks) {
        ChecklistDtos.Summary summary = toSummary(instance);
        if (summary.getMissingMandatory() == 0) return;
        ChecklistTemplate template = findTemplate(instance.getChecklistTemplateId());
        if (!Boolean.TRUE.equals(template.getAllowForwardIfIncomplete())) {
            throw new BusinessException("Forward blocked: " + summary.getMissingMandatory()
                    + " mandatory checklist response(s) are missing or rejected");
        }
        if (!override) {
            throw new BusinessException("Checklist is incomplete. Confirm override and provide remarks to forward");
        }
        requireAuthority("CHECKLIST_OVERRIDE");
        if (!StringUtils.hasText(overrideRemarks)) {
            throw new ValidationException("Checklist override remarks are required");
        }
        instance.setForwardOverrideBy(currentUserId());
        instance.setForwardOverrideAt(LocalDateTime.now());
        instance.setForwardOverrideRemarks(overrideRemarks.trim());
        instance.setStatus("FORWARDED_WITH_EXCEPTIONS");
        instanceRepository.save(instance);
        recordInstanceEvent(instance.getId(), "FORWARD_OVERRIDE", null, overrideRemarks.trim());
    }

    private List<ChecklistInstance> matchingInstances(UUID templateId, String status,
                                                        Boolean complete, Boolean missingMandatory) {
        boolean any = templateId != null || StringUtils.hasText(status)
                || complete != null || missingMandatory != null;
        if (!any) return List.of();
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
        return instanceRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(i -> templateId == null || templateId.equals(i.getChecklistTemplateId()))
                .filter(i -> normalizedStatus == null || normalizedStatus.equals(i.getStatus()))
                .filter(i -> complete == null || complete == toSummary(i).isComplete())
                .filter(i -> missingMandatory == null
                        || missingMandatory == (toSummary(i).getMissingMandatory() > 0))
                .toList();
    }

    private Optional<ChecklistTemplate> mappedTemplate(UUID categoryId) {
        if (categoryId == null) return Optional.empty();
        return mappingRepository.findByCategoryIdAndActiveTrueAndDeletedFalse(categoryId)
                .flatMap(mapping -> templateRepository.findByIdAndDeletedFalse(mapping.getChecklistTemplateId()))
                .filter(template -> Boolean.TRUE.equals(template.getActive()));
    }

    private void deleteUnusedInstance(ChecklistInstance instance) {
        boolean hasResponses = instanceItemRepository
                .findByChecklistInstanceIdAndDeletedFalseOrderBySequenceAsc(instance.getId()).stream()
                .anyMatch(item -> StringUtils.hasText(item.getResponseValueJson())
                        || attachmentRepository.countByChecklistInstanceItemIdAndActiveTrueAndDeletedFalse(item.getId()) > 0);
        if (hasResponses) {
            throw new BusinessException("Letter category cannot be changed after checklist responses are recorded");
        }
        List<ChecklistInstanceItem> items = instanceItemRepository
                .findByChecklistInstanceIdAndDeletedFalseOrderBySequenceAsc(instance.getId());
        items.forEach(item -> item.setDeleted(Boolean.TRUE));
        instanceItemRepository.saveAll(items);
        instance.setDeleted(Boolean.TRUE);
        instanceRepository.save(instance);
    }

    private void refreshStatus(ChecklistInstance instance) {
        ChecklistDtos.Summary summary = calculateSummary(instance);
        if (summary.isComplete()) {
            instance.setStatus("COMPLETE");
            if (instance.getCompletedAt() == null) instance.setCompletedAt(LocalDateTime.now());
        } else if (summary.getMissingMandatory() == 0 && summary.getPendingVerification() > 0) {
            instance.setStatus("PENDING_VERIFICATION");
            instance.setCompletedAt(null);
        } else if (instance.getForwardOverrideAt() != null) {
            instance.setStatus("FORWARDED_WITH_EXCEPTIONS");
            instance.setCompletedAt(null);
        } else {
            instance.setStatus("INCOMPLETE");
            instance.setCompletedAt(null);
        }
        instanceRepository.save(instance);
    }

    private ChecklistDtos.TemplateResponse toTemplateResponse(ChecklistTemplate template) {
        List<ChecklistDtos.ItemResponse> items = itemRepository
                .findByChecklistTemplateIdAndDeletedFalseOrderBySequenceAsc(template.getId())
                .stream().map(this::toItemResponse).toList();
        return ChecklistDtos.TemplateResponse.builder()
                .id(template.getId()).code(template.getCode()).name(template.getName())
                .description(template.getDescription())
                .allowForwardIfIncomplete(template.getAllowForwardIfIncomplete())
                .active(template.getActive()).items(items).createdAt(template.getCreatedAt()).build();
    }

    private ChecklistDtos.ItemResponse toItemResponse(ChecklistItem item) {
        return ChecklistDtos.ItemResponse.builder()
                .id(item.getId()).templateId(item.getChecklistTemplateId())
                .name(item.getItemName()).label(label(item)).description(item.getDescription())
                .controlType(item.getControlType()).placeholder(item.getPlaceholder()).helpText(item.getHelpText())
                .mandatory(item.getMandatory()).readOnly(item.getReadOnly()).visible(item.getVisible())
                .defaultValue(decode(item.getDefaultValueJson())).validationRules(decodeMap(item.getValidationRulesJson()))
                .minimumLength(item.getMinimumLength()).maximumLength(item.getMaximumLength())
                .regexPattern(item.getRegexPattern()).options(decodeList(item.getOptionsJson()))
                .autoValueExpression(item.getAutoValueExpression()).parentItemSequence(item.getParentItemSequence())
                .visibilityConditionOperator(item.getVisibilityConditionOperator())
                .visibilityConditionValue(decode(item.getVisibilityConditionValue()))
                .mandatoryConditionOperator(item.getMandatoryConditionOperator())
                .mandatoryConditionValue(decode(item.getMandatoryConditionValue()))
                .active(item.getActive()).sequence(item.getSequence()).remarksRequired(item.getRemarksRequired())
                .allowMultipleAttachments(item.getAllowMultipleAttachments())
                .supportedFileTypes(item.getSupportedFileTypes())
                .maximumFileSizeBytes(item.getMaximumFileSizeBytes()).build();
    }

    private ChecklistDtos.InstanceResponse toInstanceResponse(ChecklistInstance instance) {
        ChecklistTemplate template = findTemplate(instance.getChecklistTemplateId());
        Map<UUID, String> users = identityFacade.userNames();
        Map<UUID, String> categories = masterDataFacade.categoryNames();
        List<ChecklistInstanceItem> sourceItems = instanceItemRepository
                .findByChecklistInstanceIdAndActiveTrueAndDeletedFalseOrderBySequenceAsc(instance.getId());
        Map<Integer, ChecklistInstanceItem> bySequence = sourceItems.stream()
                .collect(Collectors.toMap(ChecklistInstanceItem::getSequence, Function.identity()));
        List<ChecklistDtos.InstanceItemResponse> items = sourceItems.stream()
                .map(item -> toInstanceItem(item, users, bySequence)).toList();
        return ChecklistDtos.InstanceResponse.builder()
                .id(instance.getId()).templateId(template.getId()).templateName(template.getName())
                .templateDescription(template.getDescription()).categoryId(instance.getCategoryId())
                .categoryName(categories.get(instance.getCategoryId()))
                .diaryEntryId(instance.getDiaryEntryId()).letterId(instance.getLetterId())
                .status(instance.getStatus())
                .allowForwardIfIncomplete(template.getAllowForwardIfIncomplete())
                .forwardOverrideBy(instance.getForwardOverrideBy())
                .forwardOverrideByName(users.get(instance.getForwardOverrideBy()))
                .forwardOverrideAt(instance.getForwardOverrideAt())
                .forwardOverrideRemarks(instance.getForwardOverrideRemarks())
                .completedAt(instance.getCompletedAt()).createdAt(instance.getCreatedAt())
                .summary(calculateSummary(instance)).items(items).timeline(timeline(instance, users)).build();
    }

    private ChecklistDtos.InstanceItemResponse toInstanceItem(ChecklistInstanceItem item,
                                                                Map<UUID, String> users,
                                                                Map<Integer, ChecklistInstanceItem> bySequence) {
        List<ChecklistDtos.AttachmentResponse> attachments = attachmentRepository
                .findByChecklistInstanceItemIdAndActiveTrueAndDeletedFalseOrderByUploadedAtAsc(item.getId())
                .stream().map(attachment -> toAttachment(attachment, users)).toList();
        return ChecklistDtos.InstanceItemResponse.builder()
                .id(item.getId()).templateItemId(item.getChecklistItemId())
                .name(item.getItemName()).label(label(item)).description(item.getDescription())
                .controlType(item.getControlType()).placeholder(item.getPlaceholder()).helpText(item.getHelpText())
                .mandatory(item.getMandatory()).effectiveMandatory(effectiveMandatory(item, bySequence))
                .readOnly(item.getReadOnly()).visible(item.getVisible()).effectiveVisible(effectiveVisible(item, bySequence))
                .defaultValue(decode(item.getDefaultValueJson())).validationRules(decodeMap(item.getValidationRulesJson()))
                .minimumLength(item.getMinimumLength()).maximumLength(item.getMaximumLength())
                .regexPattern(item.getRegexPattern()).options(decodeList(item.getOptionsJson()))
                .autoValueExpression(item.getAutoValueExpression()).parentItemSequence(item.getParentItemSequence())
                .visibilityConditionOperator(item.getVisibilityConditionOperator())
                .visibilityConditionValue(decode(item.getVisibilityConditionValue()))
                .mandatoryConditionOperator(item.getMandatoryConditionOperator())
                .mandatoryConditionValue(decode(item.getMandatoryConditionValue()))
                .sequence(item.getSequence()).remarksRequired(item.getRemarksRequired())
                .allowMultipleAttachments(item.getAllowMultipleAttachments())
                .supportedFileTypes(item.getSupportedFileTypes())
                .maximumFileSizeBytes(item.getMaximumFileSizeBytes()).status(item.getStatus())
                .responseValue(decode(item.getResponseValueJson())).responseDisplayValue(item.getResponseDisplayValue())
                .responseComplete(responseComplete(item)).respondedBy(item.getRespondedBy())
                .respondedByName(users.get(item.getRespondedBy())).respondedAt(item.getRespondedAt())
                .uploadedBy(item.getUploadedBy()).uploadedByName(users.get(item.getUploadedBy()))
                .uploadedAt(item.getUploadedAt()).verifiedBy(item.getVerifiedBy())
                .verifiedByName(users.get(item.getVerifiedBy())).verifiedAt(item.getVerifiedAt())
                .requestedBy(item.getRequestedBy()).requestedByName(users.get(item.getRequestedBy()))
                .requestedAt(item.getRequestedAt()).remarks(item.getRemarks()).attachments(attachments).build();
    }

    private ChecklistDtos.AttachmentResponse toAttachment(Attachment attachment,
                                                           Map<UUID, String> users) {
        return ChecklistDtos.AttachmentResponse.builder()
                .id(attachment.getId()).originalFileName(attachment.getOriginalFileName())
                .fileExtension(attachment.getFileExtension()).mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize()).checksum(attachment.getChecksum())
                .uploadedBy(attachment.getUploadedBy()).uploadedByName(users.get(attachment.getUploadedBy()))
                .uploadedAt(attachment.getUploadedAt())
                .downloadUrl("/api/v1/files/storage/" + attachment.getId() + "/download").build();
    }

    private ChecklistDtos.Summary toSummary(ChecklistInstance instance) {
        return calculateSummary(instance);
    }

    private ChecklistDtos.Summary calculateSummary(ChecklistInstance instance) {
        ChecklistTemplate template = findTemplate(instance.getChecklistTemplateId());
        List<ChecklistInstanceItem> allItems = instanceItemRepository
                .findByChecklistInstanceIdAndActiveTrueAndDeletedFalseOrderBySequenceAsc(instance.getId());
        Map<Integer, ChecklistInstanceItem> bySequence = allItems.stream()
                .collect(Collectors.toMap(ChecklistInstanceItem::getSequence, Function.identity()));
        List<ChecklistInstanceItem> items = allItems.stream()
                .filter(item -> effectiveVisible(item, bySequence))
                .filter(item -> !"LABEL".equals(item.getControlType()))
                .toList();
        long completed = items.stream().filter(i -> Set.of("VERIFIED", "NOT_APPLICABLE").contains(i.getStatus())).count();
        long uploaded = items.stream().filter(i -> "UPLOADED".equals(i.getStatus())).count();
        long answered = items.stream().filter(i -> "ANSWERED".equals(i.getStatus())).count();
        long pendingVerification = items.stream()
                .filter(i -> Set.of("UPLOADED", "ANSWERED").contains(i.getStatus())).count();
        long missingMandatory = items.stream().filter(i -> effectiveMandatory(i, bySequence)
                && !FORWARD_READY.contains(i.getStatus())).count();
        long pendingResponse = items.stream().filter(i -> effectiveMandatory(i, bySequence)
                && !responseReady(i)).count();
        boolean complete = items.stream().filter(i -> effectiveMandatory(i, bySequence))
                .allMatch(i -> Set.of("VERIFIED", "NOT_APPLICABLE").contains(i.getStatus()));
        return ChecklistDtos.Summary.builder()
                .instanceId(instance.getId()).templateId(template.getId()).templateName(template.getName())
                .status(instance.getStatus()).totalItems(items.size()).completedItems(completed)
                .uploadedItems(uploaded).answeredItems(answered).pendingVerification(pendingVerification)
                .pendingResponse(pendingResponse).missingMandatory(missingMandatory).complete(complete)
                .forwardOverrideUsed(instance.getForwardOverrideAt() != null).build();
    }

    private List<ChecklistDtos.TimelineEvent> timeline(ChecklistInstance instance,
                                                        Map<UUID, String> users) {
        return auditLogRepository.findByModuleAndEntityTypeAndEntityIdOrderByCreatedAtAsc(
                        MODULE, ENTITY_INSTANCE, instance.getId()).stream()
                .map(log -> ChecklistDtos.TimelineEvent.builder()
                        .timestamp(log.getCreatedAt()).action(log.getAction())
                        .title(eventTitle(log.getAction())).detail(log.getNewValue())
                        .userId(log.getUserId()).userName(users.get(log.getUserId())).build())
                .toList();
    }

    private void applyTemplate(ChecklistTemplate template, ChecklistDtos.TemplateRequest request,
                               String code) {
        template.setCode(code);
        template.setName(request.getName().trim());
        template.setDescription(blankToNull(request.getDescription()));
        template.setAllowForwardIfIncomplete(Boolean.TRUE.equals(request.getAllowForwardIfIncomplete()));
        template.setActive(request.getActive() == null || request.getActive());
    }

    private void saveTemplateItems(UUID templateId, List<ChecklistDtos.ItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ValidationException("At least one checklist item is required");
        }
        Set<Integer> sequences = new LinkedHashSet<>();
        for (ChecklistDtos.ItemRequest request : requests) {
            if (!sequences.add(request.getSequence())) {
                throw new ValidationException("Checklist item sequence must be unique");
            }
        }
        List<ChecklistItem> items = new ArrayList<>();
        for (ChecklistDtos.ItemRequest request : requests) {
            ChecklistItem item = new ChecklistItem();
            item.setChecklistTemplateId(templateId);
            item.setItemName(request.getName().trim());
            item.setDisplayLabel(blankToNull(request.getLabel()));
            item.setDescription(blankToNull(request.getDescription()));
            item.setControlType(normalizeControlType(request.getControlType()));
            item.setPlaceholder(blankToNull(request.getPlaceholder()));
            item.setHelpText(blankToNull(request.getHelpText()));
            item.setMandatory(Boolean.TRUE.equals(request.getMandatory()));
            item.setReadOnly(Boolean.TRUE.equals(request.getReadOnly()));
            item.setVisible(request.getVisible() == null || request.getVisible());
            item.setDefaultValueJson(encode(request.getDefaultValue()));
            item.setValidationRulesJson(encode(request.getValidationRules()));
            item.setMinimumLength(request.getMinimumLength());
            item.setMaximumLength(request.getMaximumLength());
            item.setRegexPattern(blankToNull(request.getRegexPattern()));
            item.setOptionsJson(encode(cleanOptions(request.getOptions())));
            item.setAutoValueExpression(blankToNull(request.getAutoValueExpression()));
            item.setParentItemSequence(request.getParentItemSequence());
            item.setVisibilityConditionOperator(normalizeOperator(request.getVisibilityConditionOperator()));
            item.setVisibilityConditionValue(encode(request.getVisibilityConditionValue()));
            item.setMandatoryConditionOperator(normalizeOperator(request.getMandatoryConditionOperator()));
            item.setMandatoryConditionValue(encode(request.getMandatoryConditionValue()));
            item.setActive(request.getActive() == null || request.getActive());
            item.setSequence(request.getSequence());
            item.setRemarksRequired(Boolean.TRUE.equals(request.getRemarksRequired()));
            item.setAllowMultipleAttachments(Boolean.TRUE.equals(request.getAllowMultipleAttachments()));
            item.setSupportedFileTypes(normalizeTypes(request.getSupportedFileTypes()));
            item.setMaximumFileSizeBytes(request.getMaximumFileSizeBytes());
            validateItemMetadata(item, sequences);
            items.add(item);
        }
        itemRepository.saveAll(items);
    }

    private ChecklistInstanceItem snapshot(ChecklistInstance instance, ChecklistItem item) {
        ChecklistInstanceItem snapshot = new ChecklistInstanceItem();
        snapshot.setChecklistInstanceId(instance.getId());
        snapshot.setChecklistItemId(item.getId());
        snapshot.setItemName(item.getItemName());
        snapshot.setDisplayLabel(item.getDisplayLabel());
        snapshot.setDescription(item.getDescription());
        snapshot.setControlType(item.getControlType());
        snapshot.setPlaceholder(item.getPlaceholder());
        snapshot.setHelpText(item.getHelpText());
        snapshot.setMandatory(item.getMandatory());
        snapshot.setReadOnly(item.getReadOnly());
        snapshot.setVisible(item.getVisible());
        snapshot.setDefaultValueJson(item.getDefaultValueJson());
        snapshot.setValidationRulesJson(item.getValidationRulesJson());
        snapshot.setMinimumLength(item.getMinimumLength());
        snapshot.setMaximumLength(item.getMaximumLength());
        snapshot.setRegexPattern(item.getRegexPattern());
        snapshot.setOptionsJson(item.getOptionsJson());
        snapshot.setAutoValueExpression(item.getAutoValueExpression());
        snapshot.setParentItemSequence(item.getParentItemSequence());
        snapshot.setVisibilityConditionOperator(item.getVisibilityConditionOperator());
        snapshot.setVisibilityConditionValue(item.getVisibilityConditionValue());
        snapshot.setMandatoryConditionOperator(item.getMandatoryConditionOperator());
        snapshot.setMandatoryConditionValue(item.getMandatoryConditionValue());
        snapshot.setSequence(item.getSequence());
        snapshot.setRemarksRequired(item.getRemarksRequired());
        snapshot.setAllowMultipleAttachments(item.getAllowMultipleAttachments());
        snapshot.setSupportedFileTypes(item.getSupportedFileTypes());
        snapshot.setMaximumFileSizeBytes(item.getMaximumFileSizeBytes());
        snapshot.setActive(Boolean.TRUE);
        applyInitialValue(instance, snapshot);
        return snapshot;
    }

    private void validateFile(ChecklistInstanceItem item, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ValidationException("Uploaded file is empty");
        if (item.getMaximumFileSizeBytes() != null && file.getSize() > item.getMaximumFileSizeBytes()) {
            throw new ValidationException("Maximum file size for " + item.getItemName() + " is "
                    + item.getMaximumFileSizeBytes() + " bytes");
        }
        if (StringUtils.hasText(item.getSupportedFileTypes())) {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            int dot = name.lastIndexOf('.');
            String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
            Set<String> allowed = Set.of(item.getSupportedFileTypes().split(","));
            if (!allowed.contains(extension)) {
                throw new ValidationException("Supported file types for " + item.getItemName()
                        + ": " + item.getSupportedFileTypes());
            }
        }
    }

    private void validateItemMetadata(ChecklistItem item, Set<Integer> sequences) {
        validateMetadata(item.getControlType(), item.getSequence(), item.getParentItemSequence(),
                item.getMinimumLength(), item.getMaximumLength(), item.getRegexPattern(),
                decodeList(item.getOptionsJson()), item.getMandatory(),
                item.getVisibilityConditionOperator(), decode(item.getVisibilityConditionValue()),
                item.getMandatoryConditionOperator(), decode(item.getMandatoryConditionValue()), sequences);
    }

    private void validateItemMetadata(ChecklistInstanceItem item, Set<Integer> sequences) {
        validateMetadata(item.getControlType(), item.getSequence(), item.getParentItemSequence(),
                item.getMinimumLength(), item.getMaximumLength(), item.getRegexPattern(),
                decodeList(item.getOptionsJson()), item.getMandatory(),
                item.getVisibilityConditionOperator(), decode(item.getVisibilityConditionValue()),
                item.getMandatoryConditionOperator(), decode(item.getMandatoryConditionValue()), sequences);
    }

//    private void validateMetadata(String controlType, Integer sequence, Integer parentSequence,
//                                  Integer minLength, Integer maxLength, String regex,
//                                  List<String> options, Boolean mandatory,
//                                  String visibilityOperator, Object visibilityValue,
//                                  String mandatoryOperator, Object mandatoryValue,
//                                  Set<Integer> sequences) {
//        if (!CONTROL_TYPES.contains(controlType)) {
//            throw new ValidationException("Unsupported checklist control type: " + controlType);
//        }
//        if (minLength != null && maxLength != null && minLength > maxLength) {
//            throw new ValidationException("Minimum length cannot exceed maximum length");
//        }
//        if (StringUtils.hasText(regex)) {
//            try { Pattern.compile(regex); }
//            catch (PatternSyntaxException ex) { throw new ValidationException("Invalid regular expression: " + ex.getMessage()); }
//        }
//        if (Set.of("DROPDOWN", "MULTI_SELECT", "RADIO_BUTTON").contains(controlType)
//                && (options == null || options.isEmpty())) {
//            throw new ValidationException("Options are required for " + controlType);
//        }
//        boolean hasVisibilityCondition = StringUtils.hasText(visibilityOperator);
//        boolean hasMandatoryCondition = StringUtils.hasText(mandatoryOperator);
//        if ((hasVisibilityCondition || hasMandatoryCondition) && parentSequence == null) {
//            throw new ValidationException("A parent item is required for conditional visibility or mandatory rules");
//        }
//        if (parentSequence != null && (!sequences.contains(parentSequence) || parentSequence >= sequence)) {
//            throw new ValidationException("Parent item must be an earlier checklist item");
//        }
//        if (parentSequence != null && !hasVisibilityCondition && !hasMandatoryCondition) {
//            throw new ValidationException("Select a visibility or mandatory condition for the parent item dependency");
//        }
//        if (VALUE_CONDITION_OPERATORS.contains(visibilityOperator) && isEmptyValue(visibilityValue)) {
//            throw new ValidationException("Visibility condition value is required for " + visibilityOperator);
//        }
//        if (VALUE_CONDITION_OPERATORS.contains(mandatoryOperator) && isEmptyValue(mandatoryValue)) {
//            throw new ValidationException("Mandatory condition value is required for " + mandatoryOperator);
//        }
//        if ("LABEL".equals(controlType) && Boolean.TRUE.equals(mandatory)) {
//            throw new ValidationException("Label / information controls cannot be mandatory");
//        }
//        if ("SIGNATURE".equals(controlType) && Boolean.TRUE.equals(mandatory)) {
//            throw new ValidationException("Signature is reserved for a future release and cannot be mandatory");
//        }
//    }

    private void validateMetadata(String controlType,
            Integer sequence,
            Integer parentSequence,
            Integer minLength,
            Integer maxLength,
            String regex,
            List<String> options,
            Boolean mandatory,
            String visibilityOperator,
            Object visibilityValue,
            String mandatoryOperator,
            Object mandatoryValue,
            Set<Integer> sequences) {

if (!CONTROL_TYPES.contains(controlType)) {
throw new ValidationException("Unsupported checklist control type: " + controlType);
}

if (minLength != null && maxLength != null && minLength > maxLength) {
throw new ValidationException("Minimum length cannot exceed maximum length");
}

if (StringUtils.hasText(regex)) {
try {
Pattern.compile(regex);
} catch (PatternSyntaxException ex) {
throw new ValidationException("Invalid regular expression: " + ex.getMessage());
}
}

if (Set.of("DROPDOWN", "MULTI_SELECT", "RADIO_BUTTON").contains(controlType)
&& (options == null || options.isEmpty())) {
throw new ValidationException("Options are required for " + controlType);
}

boolean hasVisibilityCondition = StringUtils.hasText(visibilityOperator);
boolean hasMandatoryCondition = StringUtils.hasText(mandatoryOperator);

if ((hasVisibilityCondition || hasMandatoryCondition) && parentSequence == null) {
throw new ValidationException(
"A parent item is required for conditional visibility or mandatory rules");
}

if (parentSequence != null) {
if (!sequences.contains(parentSequence) || parentSequence >= sequence) {
throw new ValidationException("Parent item must be an earlier checklist item");
}

if (!hasVisibilityCondition && !hasMandatoryCondition) {
throw new ValidationException(
  "Select a visibility or mandatory condition for the parent item dependency");
}
}

// Null-safe validation
if (hasVisibilityCondition
&& VALUE_CONDITION_OPERATORS.contains(visibilityOperator)
&& isEmptyValue(visibilityValue)) {
throw new ValidationException(
"Visibility condition value is required for " + visibilityOperator);
}

// Null-safe validation
if (hasMandatoryCondition
&& VALUE_CONDITION_OPERATORS.contains(mandatoryOperator)
&& isEmptyValue(mandatoryValue)) {
throw new ValidationException(
"Mandatory condition value is required for " + mandatoryOperator);
}

if ("LABEL".equals(controlType) && Boolean.TRUE.equals(mandatory)) {
throw new ValidationException(
"Label / information controls cannot be mandatory");
}

if ("SIGNATURE".equals(controlType) && Boolean.TRUE.equals(mandatory)) {
throw new ValidationException(
"Signature is reserved for a future release and cannot be mandatory");
}
}
    
    
    private void applyInitialValue(ChecklistInstance instance, ChecklistInstanceItem item) {
        if ("LABEL".equals(item.getControlType())) {
            item.setStatus("INFORMATION");
            return;
        }
        Object initial = decode(item.getDefaultValueJson());
        if ("AUTO_VALUE".equals(item.getControlType())) {
            initial = resolveAutoValue(item.getAutoValueExpression(), initial, instance);
            item.setReadOnly(Boolean.TRUE);
        }
        if (!isDocument(item) && !isEmptyValue(initial)) {
            item.setResponseValueJson(encode(initial));
            item.setResponseDisplayValue(displayValue(initial));
            item.setRespondedAt(LocalDateTime.now());
            item.setStatus("ANSWERED");
        } else {
            item.setStatus("PENDING");
        }
    }

    private Object resolveAutoValue(String expression, Object defaultValue, ChecklistInstance instance) {
        if (!StringUtils.hasText(expression)) return defaultValue;
        String value = expression
                .replace("${CURRENT_DATE}", LocalDate.now().toString())
                .replace("${CURRENT_DATE_TIME}", LocalDateTime.now().toString())
                .replace("${DIARY_ID}", Objects.toString(instance.getDiaryEntryId(), ""))
                .replace("${LETTER_ID}", Objects.toString(instance.getLetterId(), ""))
                .replace("${CATEGORY_ID}", Objects.toString(instance.getCategoryId(), ""));
        return value;
    }

    private void validateResponse(ChecklistInstanceItem item, Object value, boolean mandatory) {
        if (mandatory && isEmptyValue(value)) throw new ValidationException(label(item) + " is mandatory");
        if (isEmptyValue(value)) return;
        String type = item.getControlType();
        if (Set.of("YES_NO", "CHECKBOX").contains(type) && !(value instanceof Boolean)
                && !Set.of("true", "false").contains(value.toString().toLowerCase(Locale.ROOT))) {
            throw new ValidationException(label(item) + " must be Yes or No");
        }
        if ("NUMBER".equals(type)) {
            try { new BigDecimal(value.toString()); }
            catch (NumberFormatException ex) { throw new ValidationException(label(item) + " must be a number"); }
        }
        if ("DATE".equals(type)) {
            try { LocalDate.parse(value.toString()); }
            catch (RuntimeException ex) { throw new ValidationException(label(item) + " must be a valid date"); }
        }
        if ("DATE_TIME".equals(type)) {
            try { LocalDateTime.parse(value.toString()); }
            catch (RuntimeException ex) { throw new ValidationException(label(item) + " must be a valid date-time"); }
        }
        List<String> options = decodeList(item.getOptionsJson());
        if (Set.of("DROPDOWN", "RADIO_BUTTON").contains(type) && !options.contains(value.toString())) {
            throw new ValidationException("Invalid option selected for " + label(item));
        }
        if ("MULTI_SELECT".equals(type)) {
            if (!(value instanceof Collection<?> values)
                    || values.stream().anyMatch(v -> !options.contains(Objects.toString(v, "")))) {
                throw new ValidationException("Invalid option selected for " + label(item));
            }
        }
        String text = value instanceof String ? (String) value : null;
        if (text != null) {
            if (item.getMinimumLength() != null && text.length() < item.getMinimumLength()) {
                throw new ValidationException(label(item) + " requires at least " + item.getMinimumLength() + " characters");
            }
            if (item.getMaximumLength() != null && text.length() > item.getMaximumLength()) {
                throw new ValidationException(label(item) + " allows at most " + item.getMaximumLength() + " characters");
            }
            if (StringUtils.hasText(item.getRegexPattern()) && !text.matches(item.getRegexPattern())) {
                throw new ValidationException(label(item) + " does not match the configured format");
            }
        }
        Map<String, Object> rules = decodeMap(item.getValidationRulesJson());
        if ("NUMBER".equals(type)) {
            BigDecimal number = new BigDecimal(value.toString());
            if (rules.get("minimum") != null && number.compareTo(new BigDecimal(rules.get("minimum").toString())) < 0) {
                throw new ValidationException(label(item) + " is below the configured minimum");
            }
            if (rules.get("maximum") != null && number.compareTo(new BigDecimal(rules.get("maximum").toString())) > 0) {
                throw new ValidationException(label(item) + " exceeds the configured maximum");
            }
        }
    }

    private boolean effectiveVisible(ChecklistInstanceItem item, Map<Integer, ChecklistInstanceItem> bySequence) {
        if (!Boolean.TRUE.equals(item.getVisible())) return false;
        if (item.getParentItemSequence() == null || !StringUtils.hasText(item.getVisibilityConditionOperator())) return true;
        ChecklistInstanceItem parent = bySequence.get(item.getParentItemSequence());
        return parent != null && conditionMatches(decode(parent.getResponseValueJson()),
                item.getVisibilityConditionOperator(), decode(item.getVisibilityConditionValue()));
    }

    private boolean effectiveMandatory(ChecklistInstanceItem item, Map<Integer, ChecklistInstanceItem> bySequence) {
        boolean mandatory = Boolean.TRUE.equals(item.getMandatory());
        if (item.getParentItemSequence() == null || !StringUtils.hasText(item.getMandatoryConditionOperator())) return mandatory;
        ChecklistInstanceItem parent = bySequence.get(item.getParentItemSequence());
        return mandatory || (parent != null && conditionMatches(decode(parent.getResponseValueJson()),
                item.getMandatoryConditionOperator(), decode(item.getMandatoryConditionValue())));
    }

    private boolean conditionMatches(Object actual, String operator, Object expected) {
        String op = normalizeOperator(operator);
        if (op == null) return true;
        String actualText = displayValue(actual);
        String expectedText = displayValue(expected);
        return switch (op) {
            case "EQUALS" -> Objects.equals(normalizeCompare(actualText), normalizeCompare(expectedText));
            case "NOT_EQUALS" -> !Objects.equals(normalizeCompare(actualText), normalizeCompare(expectedText));
            case "TRUE" -> Boolean.parseBoolean(actualText);
            case "FALSE" -> !Boolean.parseBoolean(actualText);
            case "IS_EMPTY" -> isEmptyValue(actual);
            case "IS_NOT_EMPTY" -> !isEmptyValue(actual);
            case "CONTAINS" -> actual instanceof Collection<?> c
                    ? c.stream().anyMatch(v -> Objects.equals(normalizeCompare(displayValue(v)), normalizeCompare(expectedText)))
                    : normalizeCompare(actualText).contains(normalizeCompare(expectedText));
            case "IN" -> expected instanceof Collection<?> c
                    && c.stream().anyMatch(v -> Objects.equals(normalizeCompare(actualText), normalizeCompare(displayValue(v))));
            case "GREATER_THAN" -> compareNumbers(actual, expected) > 0;
            case "LESS_THAN" -> compareNumbers(actual, expected) < 0;
            default -> false;
        };
    }

    private int compareNumbers(Object left, Object right) {
        try { return new BigDecimal(Objects.toString(left, "0")).compareTo(new BigDecimal(Objects.toString(right, "0"))); }
        catch (NumberFormatException ex) { return 0; }
    }

    private boolean responseReady(ChecklistInstanceItem item) {
        if ("LABEL".equals(item.getControlType())) return true;
        return FORWARD_READY.contains(item.getStatus());
    }

    private boolean responseComplete(ChecklistInstanceItem item) {
        if (isDocument(item)) {
            return attachmentRepository.countByChecklistInstanceItemIdAndActiveTrueAndDeletedFalse(item.getId()) > 0;
        }
        if ("LABEL".equals(item.getControlType())) return true;
        return !isEmptyValue(decode(item.getResponseValueJson()));
    }

    private boolean isDocument(ChecklistInstanceItem item) {
        return DOCUMENT_UPLOAD.equals(item.getControlType());
    }

    private String normalizeControlType(String value) {
        String type = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : DOCUMENT_UPLOAD;
        if (!CONTROL_TYPES.contains(type)) throw new ValidationException("Unsupported checklist control type: " + type);
        return type;
    }

    private String normalizeOperator(String value) {
        if (!StringUtils.hasText(value)) return null;
        String operator = value.trim().toUpperCase(Locale.ROOT);
        if (!CONDITION_OPERATORS.contains(operator)) {
            throw new ValidationException("Unsupported checklist condition operator: " + operator);
        }
        return operator;
    }

    private List<String> cleanOptions(List<String> options) {
        if (options == null) return List.of();
        return options.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private String label(ChecklistItem item) {
        return StringUtils.hasText(item.getDisplayLabel()) ? item.getDisplayLabel() : item.getItemName();
    }

    private String label(ChecklistInstanceItem item) {
        return StringUtils.hasText(item.getDisplayLabel()) ? item.getDisplayLabel() : item.getItemName();
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) return true;
        if (value instanceof String text) return text.isBlank();
        if (value instanceof Collection<?> values) return values.isEmpty();
        return false;
    }

    private String displayValue(Object value) {
        if (value == null) return "";
        if (value instanceof Collection<?> values) return values.stream().map(v -> Objects.toString(v, ""))
                .collect(Collectors.joining(", "));
        if (value instanceof Boolean bool) return bool ? "Yes" : "No";
        return value.toString();
    }

    private String normalizeCompare(String value) {
        return Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private String encode(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new ValidationException("Invalid checklist metadata or response value"); }
    }

    private Object decode(String json) {
        if (!StringUtils.hasText(json)) return null;
        try { return objectMapper.readValue(json, Object.class); }
        catch (JsonProcessingException ex) { throw new BusinessException("Stored checklist metadata is invalid"); }
    }

    private List<String> decodeList(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() { }); }
        catch (JsonProcessingException ex) { throw new BusinessException("Stored checklist options are invalid"); }
    }

    private Map<String, Object> decodeMap(String json) {
        if (!StringUtils.hasText(json)) return new LinkedHashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { }); }
        catch (JsonProcessingException ex) { throw new BusinessException("Stored checklist validation rules are invalid"); }
    }

    private void recordInstanceEvent(UUID instanceId, String action,
                                     ChecklistInstanceItem item, String detail) {
        String value = (item != null ? "item=" + item.getItemName() + "; " : "") + detail;
        auditService.record(MODULE, ENTITY_INSTANCE, instanceId, action, null, value);
    }

    private ChecklistTemplate findTemplate(UUID id) {
        return templateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist template", id));
    }

    private ChecklistInstance findInstance(UUID id) {
        return instanceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist instance", id));
    }

    private ChecklistInstanceItem findInstanceItem(UUID id) {
        return instanceItemRepository.findByIdAndDeletedFalse(id)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item", id));
    }

    private UUID resolveSectionId(ChecklistInstance instance) {
        if (instance.getLetterId() != null) {
            return letterRepository.findByIdAndDeletedFalse(instance.getLetterId())
                    .map(Letter::getCurrentSectionId).orElse(null);
        }
        return diaryRepository.findByIdAndDeletedFalse(instance.getDiaryEntryId())
                .map(DiaryEntry::getInitialSectionId).orElse(null);
    }

    private Map<String, Double> compliance(List<SummaryRow> rows,
                                           Function<SummaryRow, String> classifier) {
        Map<String, List<SummaryRow>> groups = rows.stream()
                .collect(Collectors.groupingBy(r -> fallback(classifier.apply(r)), LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, Double> result = new LinkedHashMap<>();
        groups.forEach((name, values) -> result.put(name,
                percent(values.stream().filter(r -> r.summary().isComplete()).count(), values.size())));
        return result;
    }

    private double percent(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private void requireAuthority(String authority) {
        if (!SecurityUtils.hasAuthority(authority)) {
            throw new ForbiddenException("Missing permission: " + authority);
        }
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
    }

    private String normalizeTypes(String value) {
        if (!StringUtils.hasText(value)) return null;
        return java.util.Arrays.stream(value.toLowerCase(Locale.ROOT).split("[,\\s]+"))
                .map(type -> type.replace(".", "").trim()).filter(type -> !type.isBlank())
                .distinct().collect(Collectors.joining(","));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value : "Unassigned";
    }

    private String eventTitle(String action) {
        return switch (action) {
            case "CREATED" -> "Checklist loaded";
            case "UPLOADED" -> "Document uploaded";
            case "RESPONSE_SAVED" -> "Checklist response saved";
            case "VERIFIED" -> "Checklist item verified";
            case "REJECTED" -> "Checklist item rejected";
            case "NOT_APPLICABLE" -> "Marked not applicable";
            case "REQUESTED" -> "Additional checklist item requested";
            case "FORWARD_OVERRIDE" -> "Forwarded with checklist exception";
            case "LINKED_TO_LETTER" -> "Checklist linked to letter";
            default -> action.replace('_', ' ');
        };
    }

    private record SummaryRow(ChecklistInstance instance, ChecklistDtos.Summary summary,
                              String categoryName, String sectionName, String departmentName) { }
}
