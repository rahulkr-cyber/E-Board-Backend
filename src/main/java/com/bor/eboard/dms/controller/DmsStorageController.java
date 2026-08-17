package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.StorageConfigurationResponse;
import com.bor.eboard.dms.dto.StorageConfigurationUpdateRequest;
import com.bor.eboard.dms.dto.StorageHealthResponse;
import com.bor.eboard.dms.dto.StorageProviderResponse;
import com.bor.eboard.dms.service.DmsStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dms/storage")
@Tag(name = "DMS Storage", description = "DMS storage configuration and health")
@PreAuthorize("hasAuthority('" + DmsPermissions.ADMIN + "')")
public class DmsStorageController {

    private final DmsStorageService storageService;

    public DmsStorageController(DmsStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/configuration")
    @Operation(summary = "Get the effective DMS storage configuration")
    public ApiResponse<StorageConfigurationResponse> getConfiguration() {
        return ApiResponse.success(storageService.getConfiguration());
    }

    @PutMapping("/configuration")
    @Operation(summary = "Update and validate the active DMS storage configuration")
    public ApiResponse<StorageConfigurationResponse> updateConfiguration(
            @Valid @RequestBody StorageConfigurationUpdateRequest request) {
        return ApiResponse.success(
                "DMS storage configuration updated",
                storageService.updateConfiguration(request));
    }

    @GetMapping("/providers")
    @Operation(summary = "List registered DMS storage providers")
    public ApiResponse<List<StorageProviderResponse>> listProviders() {
        return ApiResponse.success(storageService.listProviders());
    }

    @GetMapping("/health")
    @Operation(summary = "Check the active DMS storage provider")
    public ApiResponse<StorageHealthResponse> checkHealth() {
        return ApiResponse.success(storageService.checkHealth());
    }
}
