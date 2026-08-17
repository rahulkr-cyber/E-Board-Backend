package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsMasterConfigurationOptionsResponse;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.dto.DmsMasterDataResolveRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceCreateRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceStatusRequest;
import com.bor.eboard.dms.dto.DmsMasterSourceTestResponse;
import com.bor.eboard.dms.dto.DmsMasterSourceUpdateRequest;
import com.bor.eboard.dms.service.DmsMasterSourceService;
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
@RequestMapping("/api/v1/dms/master-sources")
@Tag(name = "DMS Master Sources", description = "Configurable DMS master data sources")
public class DmsMasterSourceController {

    private static final String READ_AUTHORITIES =
            "hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
                    + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
                    + DmsPermissions.METADATA_ADMIN + "', '"
                    + DmsPermissions.MASTER_DATA_ADMIN + "', '"
                    + DmsPermissions.ADMIN + "')";

    private static final String ADMIN_AUTHORITIES =
            "hasAnyAuthority('" + DmsPermissions.MASTER_DATA_ADMIN + "', '"
                    + DmsPermissions.ADMIN + "')";

    private final DmsMasterSourceService masterSourceService;

    public DmsMasterSourceController(DmsMasterSourceService masterSourceService) {
        this.masterSourceService = masterSourceService;
    }

    @GetMapping("/configuration-options")
    @PreAuthorize(READ_AUTHORITIES)
    @Operation(summary = "List supported master source configuration options")
    public ApiResponse<DmsMasterConfigurationOptionsResponse> configurationOptions() {
        return ApiResponse.success(masterSourceService.configurationOptions());
    }

    @GetMapping
    @PreAuthorize(READ_AUTHORITIES)
    @Operation(summary = "List DMS master sources")
    public ApiResponse<List<DmsMasterSourceResponse>> findAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.success(masterSourceService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_AUTHORITIES)
    @Operation(summary = "Get a DMS master source")
    public ApiResponse<DmsMasterSourceResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(masterSourceService.findById(id));
    }

    @PostMapping
    @PreAuthorize(ADMIN_AUTHORITIES)
    @Operation(summary = "Create a DMS master source")
    public ApiResponse<DmsMasterSourceResponse> create(
            @Valid @RequestBody DmsMasterSourceCreateRequest request) {
        return ApiResponse.success(
                "DMS master source created successfully",
                masterSourceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_AUTHORITIES)
    @Operation(summary = "Update a DMS master source")
    public ApiResponse<DmsMasterSourceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody DmsMasterSourceUpdateRequest request) {
        return ApiResponse.success(
                "DMS master source updated successfully",
                masterSourceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(ADMIN_AUTHORITIES)
    @Operation(summary = "Activate or deactivate a DMS master source")
    public ApiResponse<DmsMasterSourceResponse> setActive(
            @PathVariable UUID id,
            @Valid @RequestBody DmsMasterSourceStatusRequest request) {
        String message = request.active()
                ? "DMS master source activated successfully"
                : "DMS master source deactivated successfully";
        return ApiResponse.success(message,
                masterSourceService.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_AUTHORITIES)
    @Operation(summary = "Soft-delete a DMS master source")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        masterSourceService.delete(id);
        return ApiResponse.successMessage("DMS master source deleted successfully");
    }

    @PostMapping("/{id}/options")
    @PreAuthorize(READ_AUTHORITIES)
    @Operation(summary = "Resolve master data options through the configured source")
    public ApiResponse<List<DmsMasterDataOptionResponse>> resolve(
            @PathVariable UUID id,
            @Valid @RequestBody DmsMasterDataResolveRequest request) {
        return ApiResponse.success(masterSourceService.resolve(id, request.parameters()));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize(ADMIN_AUTHORITIES)
    @Operation(summary = "Test a configured DMS master source")
    public ApiResponse<DmsMasterSourceTestResponse> test(
            @PathVariable UUID id,
            @Valid @RequestBody DmsMasterDataResolveRequest request) {
        return ApiResponse.success(
                "DMS master source test completed",
                masterSourceService.test(id, request.parameters()));
    }
}
