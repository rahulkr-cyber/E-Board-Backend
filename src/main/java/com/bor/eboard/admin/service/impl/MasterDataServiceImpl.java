package com.bor.eboard.admin.service.impl;

import com.bor.eboard.admin.dto.DocumentTypeRequest;
import com.bor.eboard.admin.dto.DocumentTypeResponse;
import com.bor.eboard.admin.dto.HolidayRequest;
import com.bor.eboard.admin.dto.HolidayResponse;
import com.bor.eboard.admin.dto.LanguageRequest;
import com.bor.eboard.admin.dto.LanguageResponse;
import com.bor.eboard.admin.dto.LetterCategoryRequest;
import com.bor.eboard.admin.dto.LetterCategoryResponse;
import com.bor.eboard.admin.dto.PriorityRequest;
import com.bor.eboard.admin.dto.PriorityResponse;
import com.bor.eboard.admin.dto.SystemSettingRequest;
import com.bor.eboard.admin.dto.SystemSettingResponse;
import com.bor.eboard.admin.entity.DocumentType;
import com.bor.eboard.admin.entity.Holiday;
import com.bor.eboard.admin.entity.Language;
import com.bor.eboard.admin.entity.LetterCategory;
import com.bor.eboard.admin.entity.Priority;
import com.bor.eboard.admin.entity.SystemSetting;
import com.bor.eboard.admin.mapper.MasterDataMapper;
import com.bor.eboard.admin.repository.DocumentTypeRepository;
import com.bor.eboard.admin.repository.HolidayRepository;
import com.bor.eboard.admin.repository.LanguageRepository;
import com.bor.eboard.admin.repository.LetterCategoryRepository;
import com.bor.eboard.admin.repository.PriorityRepository;
import com.bor.eboard.admin.repository.SystemSettingRepository;
import com.bor.eboard.admin.service.MasterDataService;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MasterDataServiceImpl implements MasterDataService {

    private static final String MODULE = "ADMIN";

    private final LetterCategoryRepository letterCategoryRepository;
    private final PriorityRepository priorityRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final LanguageRepository languageRepository;
    private final HolidayRepository holidayRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final MasterDataMapper mapper;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Letter categories
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<LetterCategoryResponse> getCategories() {
        return letterCategoryRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LetterCategoryResponse getCategory(UUID id) {
        return mapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public LetterCategoryResponse createCategory(LetterCategoryRequest request) {
        if (letterCategoryRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Category code already exists: " + request.getCode());
        }
        LetterCategory entity = new LetterCategory();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDefaultWorkflowTemplateId(request.getDefaultWorkflowTemplateId());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity = letterCategoryRepository.save(entity);
        auditService.record(MODULE, "LETTER_CATEGORY", entity.getId(),
                "CREATE", null, "code=" + entity.getCode());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public LetterCategoryResponse updateCategory(UUID id, LetterCategoryRequest request) {
        LetterCategory entity = findCategory(id);
        String oldValue = "name=" + entity.getName() + ", active=" + entity.getActive();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDefaultWorkflowTemplateId(request.getDefaultWorkflowTemplateId());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = letterCategoryRepository.save(entity);
        auditService.record(MODULE, "LETTER_CATEGORY", entity.getId(),
                "UPDATE", oldValue,
                "name=" + entity.getName() + ", active=" + entity.getActive());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        LetterCategory entity = findCategory(id);
        entity.setDeleted(Boolean.TRUE);
        letterCategoryRepository.save(entity);
        auditService.record(MODULE, "LETTER_CATEGORY", id,
                "DELETE", "code=" + entity.getCode(), "soft-deleted");
    }

    private LetterCategory findCategory(UUID id) {
        return letterCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Letter category", id));
    }

    // ------------------------------------------------------------------
    // Priorities
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PriorityResponse> getPriorities() {
        return priorityRepository.findByDeletedFalseOrderBySortOrderAsc().stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PriorityResponse getPriority(UUID id) {
        return mapper.toResponse(findPriority(id));
    }

    @Override
    @Transactional
    public PriorityResponse createPriority(PriorityRequest request) {
        if (priorityRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Priority code already exists: " + request.getCode());
        }
        Priority entity = new Priority();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setSortOrder(request.getSortOrder());
        entity.setSlaDays(request.getSlaDays());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity = priorityRepository.save(entity);
        auditService.record(MODULE, "PRIORITY", entity.getId(),
                "CREATE", null, "code=" + entity.getCode());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PriorityResponse updatePriority(UUID id, PriorityRequest request) {
        Priority entity = findPriority(id);
        String oldValue = "name=" + entity.getName()
                + ", slaDays=" + entity.getSlaDays()
                + ", active=" + entity.getActive();
        entity.setName(request.getName());
        entity.setSortOrder(request.getSortOrder());
        entity.setSlaDays(request.getSlaDays());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = priorityRepository.save(entity);
        auditService.record(MODULE, "PRIORITY", entity.getId(),
                "UPDATE", oldValue,
                "name=" + entity.getName()
                        + ", slaDays=" + entity.getSlaDays()
                        + ", active=" + entity.getActive());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deletePriority(UUID id) {
        Priority entity = findPriority(id);
        entity.setDeleted(Boolean.TRUE);
        priorityRepository.save(entity);
        auditService.record(MODULE, "PRIORITY", id,
                "DELETE", "code=" + entity.getCode(), "soft-deleted");
    }

    private Priority findPriority(UUID id) {
        return priorityRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority", id));
    }

    // ------------------------------------------------------------------
    // Document types
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> getDocumentTypes() {
        return documentTypeRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentTypeResponse getDocumentType(UUID id) {
        return mapper.toResponse(findDocumentType(id));
    }

    @Override
    @Transactional
    public DocumentTypeResponse createDocumentType(DocumentTypeRequest request) {
        if (documentTypeRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Document type code already exists: " + request.getCode());
        }
        DocumentType entity = new DocumentType();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity = documentTypeRepository.save(entity);
        auditService.record(MODULE, "DOCUMENT_TYPE", entity.getId(),
                "CREATE", null, "code=" + entity.getCode());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public DocumentTypeResponse updateDocumentType(UUID id, DocumentTypeRequest request) {
        DocumentType entity = findDocumentType(id);
        String oldValue = "name=" + entity.getName() + ", active=" + entity.getActive();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = documentTypeRepository.save(entity);
        auditService.record(MODULE, "DOCUMENT_TYPE", entity.getId(),
                "UPDATE", oldValue,
                "name=" + entity.getName() + ", active=" + entity.getActive());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteDocumentType(UUID id) {
        DocumentType entity = findDocumentType(id);
        entity.setDeleted(Boolean.TRUE);
        documentTypeRepository.save(entity);
        auditService.record(MODULE, "DOCUMENT_TYPE", id,
                "DELETE", "code=" + entity.getCode(), "soft-deleted");
    }

    private DocumentType findDocumentType(UUID id) {
        return documentTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document type", id));
    }

    // ------------------------------------------------------------------
    // Languages
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<LanguageResponse> getLanguages() {
        return languageRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LanguageResponse getLanguage(UUID id) {
        return mapper.toResponse(findLanguage(id));
    }

    @Override
    @Transactional
    public LanguageResponse createLanguage(LanguageRequest request) {
        if (languageRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Language code already exists: " + request.getCode());
        }
        Language entity = new Language();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity = languageRepository.save(entity);
        auditService.record(MODULE, "LANGUAGE", entity.getId(),
                "CREATE", null, "code=" + entity.getCode());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public LanguageResponse updateLanguage(UUID id, LanguageRequest request) {
        Language entity = findLanguage(id);
        String oldValue = "name=" + entity.getName() + ", active=" + entity.getActive();
        entity.setName(request.getName());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = languageRepository.save(entity);
        auditService.record(MODULE, "LANGUAGE", entity.getId(),
                "UPDATE", oldValue,
                "name=" + entity.getName() + ", active=" + entity.getActive());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteLanguage(UUID id) {
        Language entity = findLanguage(id);
        entity.setDeleted(Boolean.TRUE);
        languageRepository.save(entity);
        auditService.record(MODULE, "LANGUAGE", id,
                "DELETE", "code=" + entity.getCode(), "soft-deleted");
    }

    private Language findLanguage(UUID id) {
        return languageRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language", id));
    }

    // ------------------------------------------------------------------
    // Holidays
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidays(Integer year) {
        List<Holiday> holidays;
        if (year != null) {
            holidays = holidayRepository
                    .findByHolidayDateBetweenAndDeletedFalseOrderByHolidayDateAsc(
                            LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        } else {
            holidays = holidayRepository.findByDeletedFalseOrderByHolidayDateAsc();
        }
        return holidays.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HolidayResponse getHoliday(UUID id) {
        return mapper.toResponse(findHoliday(id));
    }

    @Override
    @Transactional
    public HolidayResponse createHoliday(HolidayRequest request) {
        if (holidayRepository.existsByHolidayDateAndDeletedFalse(request.getHolidayDate())) {
            throw new DuplicateResourceException(
                    "A holiday already exists on " + request.getHolidayDate());
        }
        Holiday entity = new Holiday();
        entity.setHolidayDate(request.getHolidayDate());
        entity.setName(request.getName());
        entity.setHolidayType(request.getHolidayType());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        entity = holidayRepository.save(entity);
        auditService.record(MODULE, "HOLIDAY", entity.getId(),
                "CREATE", null,
                "date=" + entity.getHolidayDate() + ", name=" + entity.getName());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public HolidayResponse updateHoliday(UUID id, HolidayRequest request) {
        Holiday entity = findHoliday(id);
        String oldValue = "date=" + entity.getHolidayDate()
                + ", name=" + entity.getName()
                + ", active=" + entity.getActive();
        entity.setHolidayDate(request.getHolidayDate());
        entity.setName(request.getName());
        entity.setHolidayType(request.getHolidayType());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = holidayRepository.save(entity);
        auditService.record(MODULE, "HOLIDAY", entity.getId(),
                "UPDATE", oldValue,
                "date=" + entity.getHolidayDate()
                        + ", name=" + entity.getName()
                        + ", active=" + entity.getActive());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteHoliday(UUID id) {
        Holiday entity = findHoliday(id);
        entity.setDeleted(Boolean.TRUE);
        holidayRepository.save(entity);
        auditService.record(MODULE, "HOLIDAY", id,
                "DELETE", "date=" + entity.getHolidayDate(), "soft-deleted");
    }

    private Holiday findHoliday(UUID id) {
        return holidayRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", id));
    }

    // ------------------------------------------------------------------
    // System settings
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSettings() {
        return systemSettingRepository.findByDeletedFalseOrderBySettingKeyAsc().stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse getSetting(UUID id) {
        return mapper.toResponse(findSetting(id));
    }

    @Override
    @Transactional
    public SystemSettingResponse createSetting(SystemSettingRequest request) {
        if (systemSettingRepository.existsBySettingKeyAndDeletedFalse(request.getSettingKey())) {
            throw new DuplicateResourceException(
                    "Setting key already exists: " + request.getSettingKey());
        }
        SystemSetting entity = new SystemSetting();
        entity.setSettingKey(request.getSettingKey());
        entity.setSettingValue(request.getSettingValue());
        entity.setDescription(request.getDescription());
        entity = systemSettingRepository.save(entity);
        auditService.record(MODULE, "SYSTEM_SETTING", entity.getId(),
                "CREATE", null, "key=" + entity.getSettingKey());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public SystemSettingResponse updateSetting(UUID id, SystemSettingRequest request) {
        SystemSetting entity = findSetting(id);
        String oldValue = "key=" + entity.getSettingKey()
                + ", value=" + entity.getSettingValue();
        entity.setSettingValue(request.getSettingValue());
        entity.setDescription(request.getDescription());
        entity = systemSettingRepository.save(entity);
        auditService.record(MODULE, "SYSTEM_SETTING", entity.getId(),
                "UPDATE", oldValue,
                "key=" + entity.getSettingKey() + ", value=" + entity.getSettingValue());
        return mapper.toResponse(entity);
    }

    private SystemSetting findSetting(UUID id) {
        return systemSettingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("System setting", id));
    }
}
