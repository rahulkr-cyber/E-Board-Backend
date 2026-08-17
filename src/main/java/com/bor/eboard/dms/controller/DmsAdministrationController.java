package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsAdministrationDashboardResponse;
import com.bor.eboard.dms.dto.DmsAdministrationHealthResponse;
import com.bor.eboard.dms.service.DmsAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dms/admin")
@Tag(name = "DMS Administration", description = "DMS operational dashboard and configuration health")
@PreAuthorize("hasAuthority('" + DmsPermissions.ADMIN + "')")
public class DmsAdministrationController {

    private final DmsAdministrationService administrationService;

    public DmsAdministrationController(DmsAdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get DMS administration dashboard metrics")
    public ApiResponse<DmsAdministrationDashboardResponse> getDashboard() {
        return ApiResponse.success(administrationService.getDashboard());
    }

    @GetMapping("/health")
    @Operation(summary = "Check DMS configuration and operational health")
    public ApiResponse<DmsAdministrationHealthResponse> checkHealth() {
        return ApiResponse.success(administrationService.checkHealth());
    }
}
