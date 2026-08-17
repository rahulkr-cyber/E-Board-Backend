package com.bor.eboard.admin.service;

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

import java.util.List;
import java.util.UUID;

/**
 * Configurable master data (02_ARCHITECTURE.md section 9).
 * Deletes are soft deletes only.
 */
public interface MasterDataService {

    // Letter categories
    List<LetterCategoryResponse> getCategories();
    LetterCategoryResponse getCategory(UUID id);
    LetterCategoryResponse createCategory(LetterCategoryRequest request);
    LetterCategoryResponse updateCategory(UUID id, LetterCategoryRequest request);
    void deleteCategory(UUID id);

    // Priorities
    List<PriorityResponse> getPriorities();
    PriorityResponse getPriority(UUID id);
    PriorityResponse createPriority(PriorityRequest request);
    PriorityResponse updatePriority(UUID id, PriorityRequest request);
    void deletePriority(UUID id);

    // Document types
    List<DocumentTypeResponse> getDocumentTypes();
    DocumentTypeResponse getDocumentType(UUID id);
    DocumentTypeResponse createDocumentType(DocumentTypeRequest request);
    DocumentTypeResponse updateDocumentType(UUID id, DocumentTypeRequest request);
    void deleteDocumentType(UUID id);

    // Languages
    List<LanguageResponse> getLanguages();
    LanguageResponse getLanguage(UUID id);
    LanguageResponse createLanguage(LanguageRequest request);
    LanguageResponse updateLanguage(UUID id, LanguageRequest request);
    void deleteLanguage(UUID id);

    // Holidays
    List<HolidayResponse> getHolidays(Integer year);
    HolidayResponse getHoliday(UUID id);
    HolidayResponse createHoliday(HolidayRequest request);
    HolidayResponse updateHoliday(UUID id, HolidayRequest request);
    void deleteHoliday(UUID id);

    // System settings
    List<SystemSettingResponse> getSettings();
    SystemSettingResponse getSetting(UUID id);
    SystemSettingResponse createSetting(SystemSettingRequest request);
    SystemSettingResponse updateSetting(UUID id, SystemSettingRequest request);
}
