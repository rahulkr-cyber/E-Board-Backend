package com.bor.eboard.dashboard.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dashboard.dto.DashboardMetrics;
import com.bor.eboard.dashboard.dto.DashboardQuery;
import com.bor.eboard.dashboard.dto.DashboardScopeOptionsResponse;
import com.bor.eboard.dashboard.dto.DashboardScopeType;
import com.bor.eboard.dashboard.dto.OverdueDashboardQuery;
import com.bor.eboard.dashboard.dto.OverdueDashboardResponse;
import com.bor.eboard.dashboard.dto.SlaTimelineEvent;
import com.bor.eboard.dashboard.service.DashboardService;
import com.bor.eboard.retirement.dto.RetirementDashboardResponse;
import com.bor.eboard.retirement.dto.RetirementPeriod;
import com.bor.eboard.retirement.dto.RetirementQuery;
import com.bor.eboard.retirement.dto.RetirementSort;
import com.bor.eboard.retirement.service.RetirementDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reusable operational dashboard. A selected scope changes only the data
 * boundary; it never changes the logged-in user, JWT, roles or authorities.
 * Legacy role-specific routes remain for backward compatibility and delegate
 * to the same scope-aware service.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Permission-scoped operational dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final RetirementDashboardService retirementDashboardService;

    @Operation(summary = "Load dashboard for a permitted view scope")
    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> dashboard(
            @RequestParam(defaultValue = "SELF") DashboardScopeType scopeType,
            @RequestParam(required = false) UUID scopeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") boolean viewAsAction) {
        return ApiResponse.success(dashboardService.get(
                new DashboardQuery(scopeType, scopeId, fromDate, toDate,
                        reason, viewAsAction)));
    }

    @Operation(summary = "Legacy retirement dashboard alias")
    @GetMapping("/retirements")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<RetirementDashboardResponse> retirements(
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

    @Operation(summary = "List dashboard scopes and human-readable targets")
    @GetMapping("/scope-options")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardScopeOptionsResponse> scopeOptions(
            @RequestParam(required = false) DashboardScopeType scopeType,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) String query) {
        return ApiResponse.success(
                dashboardService.scopeOptions(scopeType, parentId, query));
    }


    @Operation(summary = "Commissioner read-only overdue monitoring dashboard")
    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW_ORGANIZATION')")
    public ApiResponse<OverdueDashboardResponse> overdue(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID officerId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID workflowStageId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String escalationLevel,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer minDaysOverdue,
            @RequestParam(required = false) Integer maxDaysOverdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "daysOverdue") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(dashboardService.overdue(new OverdueDashboardQuery(
                departmentId, sectionId, officerId, categoryId, workflowStageId,
                priority, escalationLevel, fromDate, toDate, minDaysOverdue,
                maxDaysOverdue, page, size, sortBy, sortDir)));
    }

    @Operation(summary = "Read-only SLA timeline for an overdue workflow task")
    @GetMapping("/overdue/{taskId}/timeline")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW_ORGANIZATION')")
    public ApiResponse<java.util.List<SlaTimelineEvent>> overdueTimeline(
            @org.springframework.web.bind.annotation.PathVariable UUID taskId) {
        return ApiResponse.success(dashboardService.overdueTimeline(taskId));
    }

    @Operation(summary = "Legacy registry dashboard (personal scope)")
    @GetMapping("/registry")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> registry() {
        return ApiResponse.success(dashboardService.personal());
    }

    @Operation(summary = "Legacy section dashboard")
    @GetMapping("/section")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> section() {
        return ApiResponse.success(dashboardService.section());
    }

    @Operation(summary = "Legacy OSD dashboard")
    @GetMapping("/osd")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> osd() {
        return ApiResponse.success(dashboardService.global("OSD Dashboard"));
    }

    @Operation(summary = "Legacy Commissioner dashboard")
    @GetMapping("/commissioner")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> commissioner() {
        return ApiResponse.success(
                dashboardService.global("Commissioner Dashboard"));
    }

    @Operation(summary = "Legacy Chairperson dashboard")
    @GetMapping("/chairperson")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> chairperson() {
        return ApiResponse.success(
                dashboardService.global("Chairperson Dashboard"));
    }

    @Operation(summary = "Legacy administration dashboard")
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ApiResponse<DashboardMetrics> admin() {
        return ApiResponse.success(
                dashboardService.global("Administration Dashboard"));
    }
}
