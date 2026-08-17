package com.bor.eboard.identity.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.identity.dto.DepartmentRequest;
import com.bor.eboard.identity.dto.DepartmentResponse;
import com.bor.eboard.identity.dto.DesignationRequest;
import com.bor.eboard.identity.dto.DesignationResponse;
import com.bor.eboard.identity.dto.SectionRequest;
import com.bor.eboard.identity.dto.SectionResponse;
import com.bor.eboard.identity.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Organization master endpoints (04_API_SPEC.md section 5):
 * departments, sections, designations.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Organization", description = "Departments, sections and designations")
public class OrganizationController {

    private final OrganizationService organizationService;

    // ------------------------------------------------------------------
    // Departments
    // ------------------------------------------------------------------

    @Operation(summary = "List departments")
    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<List<DepartmentResponse>> getDepartments() {
        return ApiResponse.success(organizationService.getAllDepartments());
    }

    @Operation(summary = "Get department by id")
    @GetMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable UUID id) {
        return ApiResponse.success(organizationService.getDepartment(id));
    }

    @Operation(summary = "Create department")
    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('ORG_CREATE')")
    public ApiResponse<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created successfully",
                organizationService.createDepartment(request));
    }

    @Operation(summary = "Update department")
    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('ORG_UPDATE')")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department updated successfully",
                organizationService.updateDepartment(id, request));
    }

    @Operation(summary = "List sections of a department")
    @GetMapping("/departments/{departmentId}/sections")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<List<SectionResponse>> getSectionsByDepartment(
            @PathVariable UUID departmentId) {
        return ApiResponse.success(organizationService.getSectionsByDepartment(departmentId));
    }

    // ------------------------------------------------------------------
    // Sections
    // ------------------------------------------------------------------

    @Operation(summary = "List sections")
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<List<SectionResponse>> getSections() {
        return ApiResponse.success(organizationService.getAllSections());
    }

    @Operation(summary = "Get section by id")
    @GetMapping("/sections/{id}")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<SectionResponse> getSection(@PathVariable UUID id) {
        return ApiResponse.success(organizationService.getSection(id));
    }

    @Operation(summary = "Create section")
    @PostMapping("/sections")
    @PreAuthorize("hasAuthority('ORG_CREATE')")
    public ApiResponse<SectionResponse> createSection(
            @Valid @RequestBody SectionRequest request) {
        return ApiResponse.success("Section created successfully",
                organizationService.createSection(request));
    }

    @Operation(summary = "Update section")
    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAuthority('ORG_UPDATE')")
    public ApiResponse<SectionResponse> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody SectionRequest request) {
        return ApiResponse.success("Section updated successfully",
                organizationService.updateSection(id, request));
    }

    // ------------------------------------------------------------------
    // Designations
    // ------------------------------------------------------------------

    @Operation(summary = "List designations")
    @GetMapping("/designations")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<List<DesignationResponse>> getDesignations() {
        return ApiResponse.success(organizationService.getAllDesignations());
    }

    @Operation(summary = "Get designation by id")
    @GetMapping("/designations/{id}")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ApiResponse<DesignationResponse> getDesignation(@PathVariable UUID id) {
        return ApiResponse.success(organizationService.getDesignation(id));
    }

    @Operation(summary = "Create designation")
    @PostMapping("/designations")
    @PreAuthorize("hasAuthority('ORG_CREATE')")
    public ApiResponse<DesignationResponse> createDesignation(
            @Valid @RequestBody DesignationRequest request) {
        return ApiResponse.success("Designation created successfully",
                organizationService.createDesignation(request));
    }

    @Operation(summary = "Update designation")
    @PutMapping("/designations/{id}")
    @PreAuthorize("hasAuthority('ORG_UPDATE')")
    public ApiResponse<DesignationResponse> updateDesignation(
            @PathVariable UUID id,
            @Valid @RequestBody DesignationRequest request) {
        return ApiResponse.success("Designation updated successfully",
                organizationService.updateDesignation(id, request));
    }
}
