package com.bor.eboard.retirement.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dashboard.dto.DashboardScopeType;
import com.bor.eboard.retirement.dto.RetirementDashboardResponse;
import com.bor.eboard.retirement.dto.RetirementPeriod;
import com.bor.eboard.retirement.dto.RetirementQuery;
import com.bor.eboard.retirement.dto.RetirementSort;
import com.bor.eboard.retirement.service.RetirementDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/retirement-dashboard")
@RequiredArgsConstructor
@Tag(name = "Retirement Dashboard", description = "Permission-scoped employee retirement monitoring")
public class RetirementDashboardController {

    private final RetirementDashboardService retirementDashboardService;

    @Operation(summary = "Load the standalone retirement dashboard")
    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<RetirementDashboardResponse> dashboard(
            @RequestParam(defaultValue = "SELF") DashboardScopeType scopeType,
            @RequestParam(required = false) UUID scopeId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID designationId,
            @RequestParam(required = false) String office,
            @RequestParam(required = false) Integer retirementMonth,
            @RequestParam(required = false) Integer retirementYear,
            @RequestParam(defaultValue = "WITHIN_180_DAYS") RetirementPeriod retirementPeriod,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NEAREST_RETIREMENT") RetirementSort sortBy,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String employeeStatus,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String employeeCode) {
        return ApiResponse.success(retirementDashboardService.get(new RetirementQuery(
                scopeType, scopeId, departmentId, sectionId, designationId, office,
                retirementMonth, retirementYear, retirementPeriod, search, sortBy,
                district, employeeStatus, employeeName, employeeCode)));
    }
}
