package com.bor.eboard.identity.service;

import com.bor.eboard.identity.dto.DepartmentRequest;
import com.bor.eboard.identity.dto.DepartmentResponse;
import com.bor.eboard.identity.dto.DesignationRequest;
import com.bor.eboard.identity.dto.DesignationResponse;
import com.bor.eboard.identity.dto.SectionRequest;
import com.bor.eboard.identity.dto.SectionResponse;

import java.util.List;
import java.util.UUID;

public interface OrganizationService {

    // Departments
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse updateDepartment(UUID id, DepartmentRequest request);
    DepartmentResponse getDepartment(UUID id);
    List<DepartmentResponse> getAllDepartments();

    // Sections
    SectionResponse createSection(SectionRequest request);
    SectionResponse updateSection(UUID id, SectionRequest request);
    SectionResponse getSection(UUID id);
    List<SectionResponse> getAllSections();
    List<SectionResponse> getSectionsByDepartment(UUID departmentId);

    // Designations
    DesignationResponse createDesignation(DesignationRequest request);
    DesignationResponse updateDesignation(UUID id, DesignationRequest request);
    DesignationResponse getDesignation(UUID id);
    List<DesignationResponse> getAllDesignations();
}
