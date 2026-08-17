package com.bor.eboard.checklist.controller;

import com.bor.eboard.checklist.dto.ChecklistDtos;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.common.response.ApiResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
@Tag(name = "Checklist Engine", description = "Configurable Diary/Letter checklist module")
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('CHECKLIST_VIEW','CHECKLIST_ADMIN')")
    public ApiResponse<List<ChecklistDtos.TemplateResponse>> templates() {
        return ApiResponse.success(checklistService.getTemplates());
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyAuthority('CHECKLIST_VIEW','CHECKLIST_ADMIN')")
    public ApiResponse<ChecklistDtos.TemplateResponse> template(@PathVariable UUID id) {
        return ApiResponse.success(checklistService.getTemplate(id));
    }

    @GetMapping("/definitions/category/{categoryId}")
    @PreAuthorize("hasAnyAuthority('CHECKLIST_VIEW','CHECKLIST_UPLOAD','DIARY_CREATE')")
    @Operation(summary = "Return the complete dynamic checklist definition for a category")
    public ApiResponse<ChecklistDtos.TemplateResponse> definitionForCategory(@PathVariable UUID categoryId) {
        return ApiResponse.success(checklistService.getTemplateForCategory(categoryId).orElse(null));
    }

    @GetMapping("/definitions/templates/{id}")
    @PreAuthorize("hasAnyAuthority('CHECKLIST_VIEW','CHECKLIST_ADMIN')")
    @Operation(summary = "Return the complete dynamic checklist definition for a template")
    public ApiResponse<ChecklistDtos.TemplateResponse> definition(@PathVariable UUID id) {
        return ApiResponse.success(checklistService.getTemplate(id));
    }

    @GetMapping("/category/{categoryId}/template")
    @PreAuthorize("hasAnyAuthority('CHECKLIST_VIEW','CHECKLIST_UPLOAD','DIARY_CREATE')")
    public ApiResponse<ChecklistDtos.TemplateResponse> templateForCategory(@PathVariable UUID categoryId) {
        return ApiResponse.success(checklistService.getTemplateForCategory(categoryId).orElse(null));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('CHECKLIST_ADMIN')")
    public ApiResponse<ChecklistDtos.TemplateResponse> createTemplate(
            @Valid @RequestBody ChecklistDtos.TemplateRequest request) {
        return ApiResponse.success("Checklist template created", checklistService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('CHECKLIST_ADMIN')")
    public ApiResponse<ChecklistDtos.TemplateResponse> updateTemplate(
            @PathVariable UUID id, @Valid @RequestBody ChecklistDtos.TemplateRequest request) {
        return ApiResponse.success("Checklist template updated", checklistService.updateTemplate(id, request));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('CHECKLIST_ADMIN')")
    public ApiResponse<Void> deleteTemplate(@PathVariable UUID id) {
        checklistService.deleteTemplate(id);
        return ApiResponse.successMessage("Checklist template deleted");
    }

    @GetMapping("/mappings")
    @PreAuthorize("hasAuthority('CHECKLIST_ADMIN')")
    public ApiResponse<List<ChecklistDtos.MappingResponse>> mappings() {
        return ApiResponse.success(checklistService.getMappings());
    }

    @PostMapping("/mappings")
    @PreAuthorize("hasAuthority('CHECKLIST_ADMIN')")
    public ApiResponse<ChecklistDtos.MappingResponse> saveMapping(
            @Valid @RequestBody ChecklistDtos.MappingRequest request) {
        return ApiResponse.success("Category checklist mapping saved", checklistService.saveMapping(request));
    }

    @DeleteMapping("/mappings/category/{categoryId}")
    @PreAuthorize("hasAuthority('CHECKLIST_ADMIN')")
    public ApiResponse<Void> deleteMapping(@PathVariable UUID categoryId) {
        checklistService.deleteMapping(categoryId);
        return ApiResponse.successMessage("Category checklist mapping removed");
    }

    @GetMapping("/diary/{diaryId}")
    @PreAuthorize("hasAuthority('CHECKLIST_VIEW')")
    public ApiResponse<ChecklistDtos.InstanceResponse> forDiary(@PathVariable UUID diaryId) {
        return ApiResponse.success(checklistService.getForDiary(diaryId).orElse(null));
    }

    @GetMapping("/letter/{letterId}")
    @PreAuthorize("hasAuthority('CHECKLIST_VIEW')")
    public ApiResponse<ChecklistDtos.InstanceResponse> forLetter(@PathVariable UUID letterId) {
        return ApiResponse.success(checklistService.getForLetter(letterId).orElse(null));
    }

    @Operation(summary = "Upload a document against a checklist item")
    @PostMapping("/items/{itemId}/attachments")
    @PreAuthorize("hasAuthority('CHECKLIST_UPLOAD')")
    public ApiResponse<ChecklistDtos.InstanceResponse> upload(
            @PathVariable UUID itemId,
            @RequestParam(required = false) String remarks,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Checklist document uploaded",
                checklistService.upload(itemId, file, remarks));
    }

    @Operation(summary = "Save a value for a metadata-driven checklist control")
    @PostMapping("/items/{itemId}/response")
    @PreAuthorize("hasAuthority('CHECKLIST_UPLOAD')")
    public ApiResponse<ChecklistDtos.InstanceResponse> saveResponse(
            @PathVariable UUID itemId,
            @Valid @RequestBody ChecklistDtos.ResponseRequest request) {
        return ApiResponse.success("Checklist response saved", checklistService.saveResponse(itemId, request));
    }

    @PostMapping("/items/{itemId}/verify")
    @PreAuthorize("hasAuthority('CHECKLIST_VERIFY')")
    public ApiResponse<ChecklistDtos.InstanceResponse> verify(
            @PathVariable UUID itemId,
            @Valid @RequestBody ChecklistDtos.VerificationRequest request) {
        return ApiResponse.success("Checklist item updated", checklistService.verify(itemId, request));
    }

    @PostMapping("/instances/{instanceId}/additional-items")
    @PreAuthorize("hasAuthority('CHECKLIST_VERIFY')")
    public ApiResponse<ChecklistDtos.InstanceResponse> requestAdditional(
            @PathVariable UUID instanceId,
            @Valid @RequestBody ChecklistDtos.AdditionalItemRequest request) {
        return ApiResponse.success("Additional checklist item requested",
                checklistService.requestAdditionalItem(instanceId, request));
    }
}
