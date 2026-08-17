package com.bor.eboard.identity.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.constants.AppConstants;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.dto.AssignRolesRequest;
import com.bor.eboard.identity.dto.ChangeUserStatusRequest;
import com.bor.eboard.identity.dto.CreateUserRequest;
import com.bor.eboard.identity.dto.ResetPasswordRequest;
import com.bor.eboard.identity.dto.RoleResponse;
import com.bor.eboard.identity.dto.UpdateUserRequest;
import com.bor.eboard.identity.dto.UserResponse;
import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Designation;
import com.bor.eboard.identity.entity.Role;
import com.bor.eboard.identity.entity.Section;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.entity.UserRole;
import com.bor.eboard.identity.mapper.RoleMapper;
import com.bor.eboard.identity.mapper.UserMapper;
import com.bor.eboard.identity.repository.DepartmentRepository;
import com.bor.eboard.identity.repository.DesignationRepository;
import com.bor.eboard.identity.repository.RoleRepository;
import com.bor.eboard.identity.repository.SectionRepository;
import com.bor.eboard.identity.repository.UserRoleRepository;
import com.bor.eboard.identity.repository.UserRepository;
import com.bor.eboard.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Locale;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
/**
 * User management. All references (department, section, designation, roles)
 * are validated and resolved manually - entities carry only UUID columns.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final DesignationRepository designationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AuditService auditService;
    private final AttachmentStorageService attachmentStorageService;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmployeeCodeAndDeletedFalse(request.getEmployeeCode())) {
            throw new DuplicateResourceException(
                    "Employee code already exists: " + request.getEmployeeCode());
        }

        validateOrganizationReferences(
                request.getDepartmentId(), request.getSectionId(), request.getDesignationId());
        validateEmploymentDates(request.getDateOfBirth(), request.getDateOfJoining());
        List<Role> roles = validateRoles(request.getRoleIds());

        User user = new User();
        user.setEmployeeCode(request.getEmployeeCode());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(AppConstants.USER_STATUS_ACTIVE);
        user.setDepartmentId(request.getDepartmentId());
        user.setSectionId(request.getSectionId());
        user.setDesignationId(request.getDesignationId());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setDateOfJoining(request.getDateOfJoining());
        user.setDistrict(trimToNull(request.getDistrict()));
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(Boolean.FALSE);
        user.setAccountLockedUntil(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        user = userRepository.save(user);

        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRole.setActive(Boolean.TRUE);
            userRole.setEffectiveFrom(LocalDate.now());
            userRoleRepository.save(userRole);
        }

        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                AppConstants.AUDIT_ACTION_CREATE, null,
                "username=" + user.getUsername() + ", employeeCode=" + user.getEmployeeCode());

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        validateOrganizationReferences(
                request.getDepartmentId(), request.getSectionId(), request.getDesignationId());
        validateEmploymentDates(request.getDateOfBirth(), request.getDateOfJoining());

        String oldValue = "fullName=" + user.getFullName()
                + ", sectionId=" + user.getSectionId()
                + ", departmentId=" + user.getDepartmentId()
                + ", designationId=" + user.getDesignationId();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setDepartmentId(request.getDepartmentId());
        user.setSectionId(request.getSectionId());
        user.setDesignationId(request.getDesignationId());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setDateOfJoining(request.getDateOfJoining());
        user.setDistrict(trimToNull(request.getDistrict()));
        user = userRepository.save(user);

        String newValue = "fullName=" + user.getFullName()
                + ", sectionId=" + user.getSectionId()
                + ", departmentId=" + user.getDepartmentId()
                + ", designationId=" + user.getDesignationId();

        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                AppConstants.AUDIT_ACTION_UPDATE, oldValue, newValue);

        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(
            String query,
            UUID sectionId,
            UUID departmentId,
            String status,
            PaginationRequest pagination) {

        String normalizedQuery = StringUtils.hasText(query)
                ? "%" + query.trim().toLowerCase(Locale.ROOT) + "%"
                : null;

        String normalizedStatus = StringUtils.hasText(status)
                ? status.trim()
                : null;

        Page<User> page = userRepository.search(
                normalizedQuery,
                sectionId,
                departmentId,
                normalizedStatus,
                pagination.toPageable());

        List<UserResponse> content = toResponses(page.getContent());

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional
    public UserResponse changeStatus(UUID id, ChangeUserStatusRequest request) {
        User user = findUser(id);
        String oldStatus = user.getStatus();
        user.setStatus(request.getStatus());
        if (AppConstants.USER_STATUS_ACTIVE.equals(request.getStatus())) {
            user.setFailedLoginAttempts(0);
        }
        user = userRepository.save(user);
        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                AppConstants.AUDIT_ACTION_STATUS_CHANGE, oldStatus, request.getStatus());
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(UUID id, AssignRolesRequest request) {
        User user = findUser(id);
        List<Role> roles = validateRoles(request.getRoleIds());

        List<UserRole> existing = userRoleRepository.findByUserId(id);
        String oldValue = existing.stream()
                .filter(ur -> Boolean.TRUE.equals(ur.getActive()))
                .map(ur -> ur.getRoleId().toString())
                .collect(Collectors.joining(","));

        // Deactivate current mappings, then create fresh active mappings.
        // If an identical (user, role, effectiveFrom=today) row already exists
        // (e.g. roles reassigned twice on the same day), reactivate it instead
        // of inserting a duplicate, which would violate uq_user_roles.
        LocalDate today = LocalDate.now();
        for (UserRole userRole : existing) {
            if (Boolean.TRUE.equals(userRole.getActive())) {
                userRole.setActive(Boolean.FALSE);
                userRole.setEffectiveTo(today);
                userRoleRepository.save(userRole);
            }
        }
        for (Role role : roles) {
            UserRole sameDayRow = existing.stream()
                    .filter(ur -> ur.getRoleId().equals(role.getId()))
                    .filter(ur -> today.equals(ur.getEffectiveFrom()))
                    .findFirst()
                    .orElse(null);
            if (sameDayRow != null) {
                sameDayRow.setActive(Boolean.TRUE);
                sameDayRow.setEffectiveTo(null);
                userRoleRepository.save(sameDayRow);
            } else {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRole.setActive(Boolean.TRUE);
                userRole.setEffectiveFrom(today);
                userRoleRepository.save(userRole);
            }
        }

        String newValue = roles.stream()
                .map(r -> r.getId().toString())
                .collect(Collectors.joining(","));
        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                AppConstants.AUDIT_ACTION_ROLE_CHANGE, oldValue, newValue);

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse uploadProfilePhoto(UUID id, MultipartFile file) {
        User user = findUser(id);
        validateProfilePhoto(file);
        UUID previous = user.getProfileAttachmentId();
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                "EMPLOYEE_PROFILE", user.getId(), null, file);
        user.setProfileAttachmentId(stored.id());
        user = userRepository.save(user);
        if (previous != null && !previous.equals(stored.id())) {
            attachmentStorageService.deactivate(previous);
        }
        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                previous == null ? "PROFILE_PHOTO_UPLOAD" : "PROFILE_PHOTO_REPLACE",
                previous == null ? null : previous.toString(), stored.originalFileName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(Boolean.FALSE);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                AppConstants.AUDIT_ACTION_PASSWORD_RESET, null, "Password reset by admin");
    }

    @Override
    @Transactional
    public void unlockAccount(UUID id) {
        User user = findUser(id);
        user.setAccountLocked(Boolean.FALSE);
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        auditService.record(AppConstants.MODULE_IDENTITY, "USER", user.getId(),
                AppConstants.AUDIT_ACTION_STATUS_CHANGE, "LOCKED", "UNLOCKED");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User findUser(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void validateEmploymentDates(LocalDate dateOfBirth, LocalDate dateOfJoining) {
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth cannot be in the future");
        }
        if (dateOfBirth != null && dateOfJoining != null
                && dateOfJoining.isBefore(dateOfBirth)) {
            throw new ValidationException("Date of joining cannot be before date of birth");
        }
    }

    private void validateProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Profile photograph is required");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        boolean extensionAllowed = lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".tif") || lower.endsWith(".tiff");
        String contentType = file.getContentType();
        boolean mimeAllowed = contentType == null || contentType.startsWith("image/");
        if (!extensionAllowed || !mimeAllowed) {
            throw new ValidationException("Profile photograph must be JPG, PNG or TIFF");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateOrganizationReferences(UUID departmentId, UUID sectionId,
                                                UUID designationId) {
        departmentRepository.findByIdAndDeletedFalse(departmentId)
                .orElseThrow(() -> new ValidationException("Invalid department reference"));
        Section section = sectionRepository.findByIdAndDeletedFalse(sectionId)
                .orElseThrow(() -> new ValidationException("Invalid section reference"));
        if (!section.getDepartmentId().equals(departmentId)) {
            throw new ValidationException(
                    "Section does not belong to the selected department");
        }
        designationRepository.findByIdAndDeletedFalse(designationId)
                .orElseThrow(() -> new ValidationException("Invalid designation reference"));
    }

    private List<Role> validateRoles(List<UUID> roleIds) {
        List<Role> roles = roleRepository.findByIdInAndDeletedFalse(roleIds);
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw new ValidationException("One or more role references are invalid");
        }
        return roles;
    }

    private UserResponse toResponse(User user) {
        return toResponses(List.of(user)).get(0);
    }

    /**
     * Batch-resolves referenced departments/sections/designations/roles to
     * avoid N+1 queries (02_ARCHITECTURE.md section 29).
     */
    private List<UserResponse> toResponses(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }

        Map<UUID, Department> departments = departmentRepository
                .findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
        Map<UUID, Section> sections = sectionRepository
                .findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));
        Map<UUID, Designation> designations = designationRepository
                .findByDeletedFalseOrderByHierarchyLevelAsc().stream()
                .collect(Collectors.toMap(Designation::getId, Function.identity()));
        Map<UUID, Role> rolesById = roleRepository
                .findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        LocalDate today = LocalDate.now();
        List<UserResponse> responses = new ArrayList<>();
        for (User user : users) {
            Map<UUID, Role> userRoles = new HashMap<>();
            userRoleRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                    .filter(ur -> ur.getEffectiveFrom() == null
                            || !ur.getEffectiveFrom().isAfter(today))
                    .filter(ur -> ur.getEffectiveTo() == null
                            || !ur.getEffectiveTo().isBefore(today))
                    .forEach(ur -> {
                        Role role = rolesById.get(ur.getRoleId());
                        if (role != null) {
                            userRoles.put(role.getId(), role);
                        }
                    });
            List<RoleResponse> roleResponses = userRoles.values().stream()
                    .map(roleMapper::toResponse)
                    .toList();
            responses.add(userMapper.toResponse(
                    user,
                    user.getDepartmentId() != null ? departments.get(user.getDepartmentId()) : null,
                    user.getSectionId() != null ? sections.get(user.getSectionId()) : null,
                    user.getDesignationId() != null ? designations.get(user.getDesignationId()) : null,
                    roleResponses));
        }
        return responses;
    }
}
