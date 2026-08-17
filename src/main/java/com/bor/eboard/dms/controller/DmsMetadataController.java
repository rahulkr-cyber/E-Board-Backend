package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsMetadataConfigurationOptionsResponse;
import com.bor.eboard.dms.dto.DmsMetadataFieldCreateRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldOrderRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldResponse;
import com.bor.eboard.dms.dto.DmsMetadataFieldStatusRequest;
import com.bor.eboard.dms.dto.DmsMetadataFieldUpdateRequest;
import com.bor.eboard.dms.dto.DmsMetadataValidationRequest;
import com.bor.eboard.dms.dto.DmsMetadataValidationResponse;
import com.bor.eboard.dms.service.DmsMetadataService;
import com.bor.eboard.dms.service.DmsMetadataValidationService;
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
@RequestMapping("/api/v1/dms")
@Tag(name = "DMS Metadata", description = "Configurable DMS metadata administration and validation")
public class DmsMetadataController {

    private static final String METADATA_READ_AUTHORITIES =
            "hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
                    + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
                    + DmsPermissions.METADATA_ADMIN + "', '" + DmsPermissions.ADMIN + "')";

    private static final String METADATA_ADMIN_AUTHORITIES =
            "hasAnyAuthority('" + DmsPermissions.METADATA_ADMIN + "', '"
                    + DmsPermissions.ADMIN + "')";

    private final DmsMetadataService metadataService;
    private final DmsMetadataValidationService validationService;

    public DmsMetadataController(
            DmsMetadataService metadataService,
            DmsMetadataValidationService validationService) {
        this.metadataService = metadataService;
        this.validationService = validationService;
    }

    @GetMapping("/metadata/configuration-options")
    @PreAuthorize(METADATA_READ_AUTHORITIES)
    @Operation(summary = "List supported metadata controls and validation options")
    public ApiResponse<DmsMetadataConfigurationOptionsResponse> configurationOptions() {
        return ApiResponse.success(metadataService.configurationOptions());
    }

    @GetMapping("/document-types/{documentTypeId}/metadata-fields")
    @PreAuthorize(METADATA_READ_AUTHORITIES)
    @Operation(summary = "List metadata fields for a DMS document type")
    public ApiResponse<List<DmsMetadataFieldResponse>> findAll(
            @PathVariable UUID documentTypeId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.success(metadataService.findAll(documentTypeId, activeOnly));
    }

    @GetMapping("/metadata-fields/{id}")
    @PreAuthorize(METADATA_READ_AUTHORITIES)
    @Operation(summary = "Get a DMS metadata field")
    public ApiResponse<DmsMetadataFieldResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(metadataService.findById(id));
    }

    @PostMapping("/document-types/{documentTypeId}/metadata-fields")
    @PreAuthorize(METADATA_ADMIN_AUTHORITIES)
    @Operation(summary = "Create a DMS metadata field")
    public ApiResponse<DmsMetadataFieldResponse> create(
            @PathVariable UUID documentTypeId,
            @Valid @RequestBody DmsMetadataFieldCreateRequest request) {
        return ApiResponse.success(
                "DMS metadata field created successfully",
                metadataService.create(documentTypeId, request));
    }

    @PutMapping("/metadata-fields/{id}")
    @PreAuthorize(METADATA_ADMIN_AUTHORITIES)
    @Operation(summary = "Update a DMS metadata field")
    public ApiResponse<DmsMetadataFieldResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody DmsMetadataFieldUpdateRequest request) {
        return ApiResponse.success(
                "DMS metadata field updated successfully",
                metadataService.update(id, request));
    }

    @PatchMapping("/metadata-fields/{id}/status")
    @PreAuthorize(METADATA_ADMIN_AUTHORITIES)
    @Operation(summary = "Activate or deactivate a DMS metadata field")
    public ApiResponse<DmsMetadataFieldResponse> setActive(
            @PathVariable UUID id,
            @Valid @RequestBody DmsMetadataFieldStatusRequest request) {
        String message = request.active()
                ? "DMS metadata field activated successfully"
                : "DMS metadata field deactivated successfully";
        return ApiResponse.success(message,
                metadataService.setActive(id, request.active()));
    }

    @PutMapping("/document-types/{documentTypeId}/metadata-fields/order")
    @PreAuthorize(METADATA_ADMIN_AUTHORITIES)
    @Operation(summary = "Reorder DMS metadata fields")
    public ApiResponse<List<DmsMetadataFieldResponse>> reorder(
            @PathVariable UUID documentTypeId,
            @Valid @RequestBody DmsMetadataFieldOrderRequest request) {
        return ApiResponse.success(
                "DMS metadata fields reordered successfully",
                metadataService.reorder(documentTypeId, request));
    }

    @DeleteMapping("/metadata-fields/{id}")
    @PreAuthorize(METADATA_ADMIN_AUTHORITIES)
    @Operation(summary = "Soft-delete a DMS metadata field")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        metadataService.delete(id);
        return ApiResponse.successMessage("DMS metadata field deleted successfully");
    }

    @PostMapping("/document-types/{documentTypeId}/metadata/validate")
    @PreAuthorize(METADATA_READ_AUTHORITIES)
    @Operation(summary = "Validate metadata values against the configured definition")
    public ApiResponse<DmsMetadataValidationResponse> validate(
            @PathVariable UUID documentTypeId,
            @Valid @RequestBody DmsMetadataValidationRequest request) {
        return ApiResponse.success(validationService.validate(documentTypeId, request.values()));
    }
}
