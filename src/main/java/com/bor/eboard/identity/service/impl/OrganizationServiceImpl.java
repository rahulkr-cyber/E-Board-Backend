package com.bor.eboard.identity.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.constants.AppConstants;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.identity.dto.DepartmentRequest;
import com.bor.eboard.identity.dto.DepartmentResponse;
import com.bor.eboard.identity.dto.DesignationRequest;
import com.bor.eboard.identity.dto.DesignationResponse;
import com.bor.eboard.identity.dto.SectionRequest;
import com.bor.eboard.identity.dto.SectionResponse;
import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Designation;
import com.bor.eboard.identity.entity.Section;
import com.bor.eboard.identity.mapper.OrganizationMapper;
import com.bor.eboard.identity.repository.DepartmentRepository;
import com.bor.eboard.identity.repository.DesignationRepository;
import com.bor.eboard.identity.repository.SectionRepository;
import com.bor.eboard.identity.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final DesignationRepository designationRepository;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Departments
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Department code already exists: " + request.getCode());
        }
        Department department = new Department();
        department.setCode(request.getCode());
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        department = departmentRepository.save(department);

        auditService.record(AppConstants.MODULE_ORGANIZATION, "DEPARTMENT",
                department.getId(), AppConstants.AUDIT_ACTION_CREATE, null,
                "code=" + department.getCode());

        return organizationMapper.toResponse(department);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        Department department = findDepartment(id);
        String oldValue = "name=" + department.getName() + ", active=" + department.getActive();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }
        department = departmentRepository.save(department);

        auditService.record(AppConstants.MODULE_ORGANIZATION, "DEPARTMENT",
                department.getId(), AppConstants.AUDIT_ACTION_UPDATE, oldValue,
                "name=" + department.getName() + ", active=" + department.getActive());

        return organizationMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID id) {
        return organizationMapper.toResponse(findDepartment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(organizationMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Sections
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public SectionResponse createSection(SectionRequest request) {
        Department department = departmentRepository
                .findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("Invalid department reference"));
        if (sectionRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Section code already exists: " + request.getCode());
        }
        Section section = new Section();
        section.setDepartmentId(request.getDepartmentId());
        section.setCode(request.getCode());
        section.setName(request.getName());
        section.setDescription(request.getDescription());
        section.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        section = sectionRepository.save(section);

        auditService.record(AppConstants.MODULE_ORGANIZATION, "SECTION",
                section.getId(), AppConstants.AUDIT_ACTION_CREATE, null,
                "code=" + section.getCode());

        return organizationMapper.toResponse(section, department);
    }

    @Override
    @Transactional
    public SectionResponse updateSection(UUID id, SectionRequest request) {
        Section section = findSection(id);
        Department department = departmentRepository
                .findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("Invalid department reference"));

        String oldValue = "name=" + section.getName()
                + ", departmentId=" + section.getDepartmentId()
                + ", active=" + section.getActive();

        section.setDepartmentId(request.getDepartmentId());
        section.setName(request.getName());
        section.setDescription(request.getDescription());
        if (request.getActive() != null) {
            section.setActive(request.getActive());
        }
        section = sectionRepository.save(section);

        auditService.record(AppConstants.MODULE_ORGANIZATION, "SECTION",
                section.getId(), AppConstants.AUDIT_ACTION_UPDATE, oldValue,
                "name=" + section.getName()
                        + ", departmentId=" + section.getDepartmentId()
                        + ", active=" + section.getActive());

        return organizationMapper.toResponse(section, department);
    }

    @Override
    @Transactional(readOnly = true)
    public SectionResponse getSection(UUID id) {
        Section section = findSection(id);
        Department department = departmentRepository
                .findByIdAndDeletedFalse(section.getDepartmentId()).orElse(null);
        return organizationMapper.toResponse(section, department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getAllSections() {
        return mapSections(sectionRepository.findByDeletedFalseOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByDepartment(UUID departmentId) {
        return mapSections(sectionRepository
                .findByDepartmentIdAndDeletedFalseOrderByNameAsc(departmentId));
    }

    // ------------------------------------------------------------------
    // Designations
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public DesignationResponse createDesignation(DesignationRequest request) {
        if (designationRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Designation code already exists: " + request.getCode());
        }
        Designation designation = new Designation();
        designation.setCode(request.getCode());
        designation.setName(request.getName());
        designation.setHierarchyLevel(request.getHierarchyLevel());
        designation.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        designation = designationRepository.save(designation);

        auditService.record(AppConstants.MODULE_ORGANIZATION, "DESIGNATION",
                designation.getId(), AppConstants.AUDIT_ACTION_CREATE, null,
                "code=" + designation.getCode());

        return organizationMapper.toResponse(designation);
    }

    @Override
    @Transactional
    public DesignationResponse updateDesignation(UUID id, DesignationRequest request) {
        Designation designation = findDesignation(id);
        String oldValue = "name=" + designation.getName()
                + ", hierarchyLevel=" + designation.getHierarchyLevel()
                + ", active=" + designation.getActive();

        designation.setName(request.getName());
        designation.setHierarchyLevel(request.getHierarchyLevel());
        if (request.getActive() != null) {
            designation.setActive(request.getActive());
        }
        designation = designationRepository.save(designation);

        auditService.record(AppConstants.MODULE_ORGANIZATION, "DESIGNATION",
                designation.getId(), AppConstants.AUDIT_ACTION_UPDATE, oldValue,
                "name=" + designation.getName()
                        + ", hierarchyLevel=" + designation.getHierarchyLevel()
                        + ", active=" + designation.getActive());

        return organizationMapper.toResponse(designation);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationResponse getDesignation(UUID id) {
        return organizationMapper.toResponse(findDesignation(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllDesignations() {
        return designationRepository.findByDeletedFalseOrderByHierarchyLevelAsc().stream()
                .map(organizationMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Department findDepartment(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private Section findSection(UUID id) {
        return sectionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", id));
    }

    private Designation findDesignation(UUID id) {
        return designationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", id));
    }

    private List<SectionResponse> mapSections(List<Section> sections) {
        Map<UUID, Department> departments = departmentRepository
                .findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
        return sections.stream()
                .map(section -> organizationMapper.toResponse(
                        section, departments.get(section.getDepartmentId())))
                .toList();
    }
}
