package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.dms.dto.DmsAccessPrincipalResponse;
import com.bor.eboard.dms.security.DmsPrincipalType;
import com.bor.eboard.dms.service.DmsAccessPrincipalService;
import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Designation;
import com.bor.eboard.identity.entity.Role;
import com.bor.eboard.identity.entity.Section;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.repository.DepartmentRepository;
import com.bor.eboard.identity.repository.DesignationRepository;
import com.bor.eboard.identity.repository.RoleRepository;
import com.bor.eboard.identity.repository.SectionRepository;
import com.bor.eboard.identity.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class DmsAccessPrincipalServiceImpl implements DmsAccessPrincipalService {

    private static final int MAX_RESULTS = 50;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final DesignationRepository designationRepository;

    public DmsAccessPrincipalServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            SectionRepository sectionRepository,
            DesignationRepository designationRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.designationRepository = designationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsAccessPrincipalResponse> search(
            DmsPrincipalType principalType,
            String query,
            UUID departmentId) {
        String normalized = normalizeQuery(query);
        return switch (principalType) {
            case USER -> searchUsers(normalized, departmentId);
            case ROLE -> roleRepository.findByDeletedFalseOrderByNameAsc().stream()
                    .filter(role -> Boolean.TRUE.equals(role.getActive()))
                    .filter(role -> matches(normalized, role.getCode(), role.getName()))
                    .limit(MAX_RESULTS)
                    .map(this::toResponse)
                    .toList();
            case DEPARTMENT -> departmentRepository.findByDeletedFalseOrderByNameAsc().stream()
                    .filter(department -> Boolean.TRUE.equals(department.getActive()))
                    .filter(department -> matches(normalized, department.getCode(), department.getName()))
                    .limit(MAX_RESULTS)
                    .map(this::toResponse)
                    .toList();
            case SECTION -> (departmentId == null
                    ? sectionRepository.findByDeletedFalseOrderByNameAsc()
                    : sectionRepository.findByDepartmentIdAndDeletedFalseOrderByNameAsc(departmentId))
                    .stream()
                    .filter(section -> Boolean.TRUE.equals(section.getActive()))
                    .filter(section -> matches(normalized, section.getCode(), section.getName()))
                    .limit(MAX_RESULTS)
                    .map(this::toResponse)
                    .toList();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public String requireName(DmsPrincipalType principalType, UUID principalId) {
        if (principalType == null || principalId == null) {
            throw new ResourceNotFoundException("DMS access principal was not found");
        }
        return switch (principalType) {
            case USER -> {
                User user = userRepository.findByIdAndDeletedFalse(principalId)
                        .filter(value -> "ACTIVE".equalsIgnoreCase(value.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException("Active user", principalId));
                yield user.getFullName();
            }
            case ROLE -> {
                Role role = roleRepository.findByIdAndDeletedFalse(principalId)
                        .filter(value -> Boolean.TRUE.equals(value.getActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("Active role", principalId));
                yield role.getName();
            }
            case DEPARTMENT -> {
                Department department = departmentRepository.findByIdAndDeletedFalse(principalId)
                        .filter(value -> Boolean.TRUE.equals(value.getActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("Active department", principalId));
                yield department.getName();
            }
            case SECTION -> {
                Section section = sectionRepository.findByIdAndDeletedFalse(principalId)
                        .filter(value -> Boolean.TRUE.equals(value.getActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("Active section", principalId));
                yield section.getName();
            }
        };
    }

    private List<DmsAccessPrincipalResponse> searchUsers(String query, UUID departmentId) {
        String repositoryQuery = query == null ? null : "%" + query + "%";
        List<User> users = userRepository.search(
                        repositoryQuery,
                        null,
                        departmentId,
                        "ACTIVE",
                        PageRequest.of(0, MAX_RESULTS, Sort.by("fullName").ascending()))
                .getContent().stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<UUID, String> designationNames = designationRepository
                .findByDeletedFalseOrderByHierarchyLevelAsc().stream()
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .collect(Collectors.toMap(Designation::getId, Designation::getName,
                        (left, right) -> left));
        return users.stream()
                .map(user -> toResponse(user, designationNames))
                .toList();
    }

    private DmsAccessPrincipalResponse toResponse(
            User value,
            Map<UUID, String> designationNames) {
        return new DmsAccessPrincipalResponse(
                value.getId(), DmsPrincipalType.USER,
                value.getEmployeeCode(), value.getFullName(), value.getDepartmentId(),
                designationNames.get(value.getDesignationId()));
    }

    private DmsAccessPrincipalResponse toResponse(Role value) {
        return new DmsAccessPrincipalResponse(
                value.getId(), DmsPrincipalType.ROLE,
                value.getCode(), value.getName(), null, null);
    }

    private DmsAccessPrincipalResponse toResponse(Department value) {
        return new DmsAccessPrincipalResponse(
                value.getId(), DmsPrincipalType.DEPARTMENT,
                value.getCode(), value.getName(), value.getId(), null);
    }

    private DmsAccessPrincipalResponse toResponse(Section value) {
        return new DmsAccessPrincipalResponse(
                value.getId(), DmsPrincipalType.SECTION,
                value.getCode(), value.getName(), value.getDepartmentId(), null);
    }

    private boolean matches(String query, String code, String name) {
        return query == null
                || (code != null && code.toLowerCase(Locale.ROOT).contains(query))
                || (name != null && name.toLowerCase(Locale.ROOT).contains(query));
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
