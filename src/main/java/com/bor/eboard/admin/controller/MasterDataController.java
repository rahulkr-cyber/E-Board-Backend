package com.bor.eboard.admin.controller;

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
import com.bor.eboard.admin.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bor.eboard.common.response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Master data administration (02_ARCHITECTURE.md section 9).
 * Reads are open to any authenticated user (masters feed dropdowns across
 * all modules); writes require MASTER_MANAGE. Deletes are soft deletes.
 */
@RestController
@RequestMapping("/api/v1/masters")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Categories, priorities, document types, languages, holidays, system settings")
public class MasterDataController {

    private final MasterDataService masterDataService;

    // ------------------------------------------------------------------
    // Letter categories
    // ------------------------------------------------------------------

    @Operation(summary = "List letter categories")
    @GetMapping("/categories")
    public ApiResponse<List<LetterCategoryResponse>> getCategories() {
        return ApiResponse.success(masterDataService.getCategories());
    }

    @Operation(summary = "Get letter category by id")
    @GetMapping("/categories/{id}")
    public ApiResponse<LetterCategoryResponse> getCategory(@PathVariable UUID id) {
        return ApiResponse.success(masterDataService.getCategory(id));
    }

    @Operation(summary = "Create letter category")
    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<LetterCategoryResponse> createCategory(
            @Valid @RequestBody LetterCategoryRequest request) {
        return ApiResponse.success("Category created successfully",
                masterDataService.createCategory(request));
    }

    @Operation(summary = "Update letter category")
    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<LetterCategoryResponse> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody LetterCategoryRequest request) {
        return ApiResponse.success("Category updated successfully",
                masterDataService.updateCategory(id, request));
    }

    @Operation(summary = "Delete letter category (soft delete)")
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<Void> deleteCategory(@PathVariable UUID id) {
        masterDataService.deleteCategory(id);
        return ApiResponse.successMessage("Category deleted successfully");
    }

    // ------------------------------------------------------------------
    // Priorities
    // ------------------------------------------------------------------

    @Operation(summary = "List priorities")
    @GetMapping("/priorities")
    public ApiResponse<List<PriorityResponse>> getPriorities() {
        return ApiResponse.success(masterDataService.getPriorities());
    }

    @Operation(summary = "Get priority by id")
    @GetMapping("/priorities/{id}")
    public ApiResponse<PriorityResponse> getPriority(@PathVariable UUID id) {
        return ApiResponse.success(masterDataService.getPriority(id));
    }

    @Operation(summary = "Create priority")
    @PostMapping("/priorities")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<PriorityResponse> createPriority(
            @Valid @RequestBody PriorityRequest request) {
        return ApiResponse.success("Priority created successfully",
                masterDataService.createPriority(request));
    }

    @Operation(summary = "Update priority")
    @PutMapping("/priorities/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<PriorityResponse> updatePriority(
            @PathVariable UUID id, @Valid @RequestBody PriorityRequest request) {
        return ApiResponse.success("Priority updated successfully",
                masterDataService.updatePriority(id, request));
    }

    @Operation(summary = "Delete priority (soft delete)")
    @DeleteMapping("/priorities/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<Void> deletePriority(@PathVariable UUID id) {
        masterDataService.deletePriority(id);
        return ApiResponse.successMessage("Priority deleted successfully");
    }

    // ------------------------------------------------------------------
    // Document types
    // ------------------------------------------------------------------

    @Operation(summary = "List document types")
    @GetMapping("/document-types")
    public ApiResponse<List<DocumentTypeResponse>> getDocumentTypes() {
        return ApiResponse.success(masterDataService.getDocumentTypes());
    }

    @Operation(summary = "Get document type by id")
    @GetMapping("/document-types/{id}")
    public ApiResponse<DocumentTypeResponse> getDocumentType(@PathVariable UUID id) {
        return ApiResponse.success(masterDataService.getDocumentType(id));
    }

    @Operation(summary = "Create document type")
    @PostMapping("/document-types")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<DocumentTypeResponse> createDocumentType(
            @Valid @RequestBody DocumentTypeRequest request) {
        return ApiResponse.success("Document type created successfully",
                masterDataService.createDocumentType(request));
    }

    @Operation(summary = "Update document type")
    @PutMapping("/document-types/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<DocumentTypeResponse> updateDocumentType(
            @PathVariable UUID id, @Valid @RequestBody DocumentTypeRequest request) {
        return ApiResponse.success("Document type updated successfully",
                masterDataService.updateDocumentType(id, request));
    }

    @Operation(summary = "Delete document type (soft delete)")
    @DeleteMapping("/document-types/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<Void> deleteDocumentType(@PathVariable UUID id) {
        masterDataService.deleteDocumentType(id);
        return ApiResponse.successMessage("Document type deleted successfully");
    }

    // ------------------------------------------------------------------
    // Languages
    // ------------------------------------------------------------------

    @Operation(summary = "List languages")
    @GetMapping("/languages")
    public ApiResponse<List<LanguageResponse>> getLanguages() {
        return ApiResponse.success(masterDataService.getLanguages());
    }

    @Operation(summary = "Get language by id")
    @GetMapping("/languages/{id}")
    public ApiResponse<LanguageResponse> getLanguage(@PathVariable UUID id) {
        return ApiResponse.success(masterDataService.getLanguage(id));
    }

    @Operation(summary = "Create language")
    @PostMapping("/languages")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<LanguageResponse> createLanguage(
            @Valid @RequestBody LanguageRequest request) {
        return ApiResponse.success("Language created successfully",
                masterDataService.createLanguage(request));
    }

    @Operation(summary = "Update language")
    @PutMapping("/languages/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<LanguageResponse> updateLanguage(
            @PathVariable UUID id, @Valid @RequestBody LanguageRequest request) {
        return ApiResponse.success("Language updated successfully",
                masterDataService.updateLanguage(id, request));
    }

    @Operation(summary = "Delete language (soft delete)")
    @DeleteMapping("/languages/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<Void> deleteLanguage(@PathVariable UUID id) {
        masterDataService.deleteLanguage(id);
        return ApiResponse.successMessage("Language deleted successfully");
    }

    // ------------------------------------------------------------------
    // Holidays
    // ------------------------------------------------------------------

    @Operation(summary = "List holidays (optionally filtered by year)")
    @GetMapping("/holidays")
    public ApiResponse<List<HolidayResponse>> getHolidays(
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(masterDataService.getHolidays(year));
    }

    @Operation(summary = "Get holiday by id")
    @GetMapping("/holidays/{id}")
    public ApiResponse<HolidayResponse> getHoliday(@PathVariable UUID id) {
        return ApiResponse.success(masterDataService.getHoliday(id));
    }

    @Operation(summary = "Create holiday")
    @PostMapping("/holidays")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<HolidayResponse> createHoliday(
            @Valid @RequestBody HolidayRequest request) {
        return ApiResponse.success("Holiday created successfully",
                masterDataService.createHoliday(request));
    }

    @Operation(summary = "Update holiday")
    @PutMapping("/holidays/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<HolidayResponse> updateHoliday(
            @PathVariable UUID id, @Valid @RequestBody HolidayRequest request) {
        return ApiResponse.success("Holiday updated successfully",
                masterDataService.updateHoliday(id, request));
    }

    @Operation(summary = "Delete holiday (soft delete)")
    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<Void> deleteHoliday(@PathVariable UUID id) {
        masterDataService.deleteHoliday(id);
        return ApiResponse.successMessage("Holiday deleted successfully");
    }

    // ------------------------------------------------------------------
    // System settings
    // ------------------------------------------------------------------

    @Operation(summary = "List system settings")
    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<List<SystemSettingResponse>> getSettings() {
        return ApiResponse.success(masterDataService.getSettings());
    }

    @Operation(summary = "Get system setting by id")
    @GetMapping("/settings/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<SystemSettingResponse> getSetting(@PathVariable UUID id) {
        return ApiResponse.success(masterDataService.getSetting(id));
    }

    @Operation(summary = "Create system setting")
    @PostMapping("/settings")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<SystemSettingResponse> createSetting(
            @Valid @RequestBody SystemSettingRequest request) {
        return ApiResponse.success("Setting created successfully",
                masterDataService.createSetting(request));
    }

    @Operation(summary = "Update system setting value")
    @PutMapping("/settings/{id}")
    @PreAuthorize("hasAuthority('MASTER_MANAGE')")
    public ApiResponse<SystemSettingResponse> updateSetting(
            @PathVariable UUID id, @Valid @RequestBody SystemSettingRequest request) {
        return ApiResponse.success("Setting updated successfully",
                masterDataService.updateSetting(id, request));
    }
}
