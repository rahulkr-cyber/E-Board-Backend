package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsDocumentTypeCreateRequest;
import com.bor.eboard.dms.dto.DmsDocumentTypeResponse;
import com.bor.eboard.dms.dto.DmsDocumentTypeStatusRequest;
import com.bor.eboard.dms.dto.DmsDocumentTypeUpdateRequest;
import com.bor.eboard.dms.service.DmsDocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dms/document-types")
@Tag(name = "DMS Document Types", description = "Configurable DMS document type administration")
public class DmsDocumentTypeController {

    private final DmsDocumentTypeService documentTypeService;

    public DmsDocumentTypeController(DmsDocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.DOCUMENT_TYPE_ADMIN + "')")
    @Operation(summary = "List DMS document types")
    public ApiResponse<List<DmsDocumentTypeResponse>> findAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.success(documentTypeService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.DOCUMENT_TYPE_ADMIN + "')")
    @Operation(summary = "Get a DMS document type")
    public ApiResponse<DmsDocumentTypeResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(documentTypeService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + DmsPermissions.DOCUMENT_TYPE_ADMIN + "')")
    @Operation(summary = "Create a DMS document type")
    public ApiResponse<DmsDocumentTypeResponse> create(
            @Valid @RequestBody DmsDocumentTypeCreateRequest request) {
        return ApiResponse.success(
                "DMS document type created successfully",
                documentTypeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + DmsPermissions.DOCUMENT_TYPE_ADMIN + "')")
    @Operation(summary = "Update a DMS document type")
    public ApiResponse<DmsDocumentTypeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody DmsDocumentTypeUpdateRequest request) {
        return ApiResponse.success(
                "DMS document type updated successfully",
                documentTypeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + DmsPermissions.DOCUMENT_TYPE_ADMIN + "')")
    @Operation(summary = "Activate or deactivate a DMS document type")
    public ApiResponse<DmsDocumentTypeResponse> setActive(
            @PathVariable UUID id,
            @Valid @RequestBody DmsDocumentTypeStatusRequest request) {
        String message = request.active()
                ? "DMS document type activated successfully"
                : "DMS document type deactivated successfully";
        return ApiResponse.success(message,
                documentTypeService.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + DmsPermissions.DOCUMENT_TYPE_ADMIN + "')")
    @Operation(summary = "Soft-delete a DMS document type")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        documentTypeService.delete(id);
        return ApiResponse.successMessage("DMS document type deleted successfully");
    }
}
