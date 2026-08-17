package com.bor.eboard.identity.facade.impl;

import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Role;
import com.bor.eboard.identity.entity.Section;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.entity.UserRole;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.identity.repository.DepartmentRepository;
import com.bor.eboard.identity.repository.DesignationRepository;
import com.bor.eboard.identity.repository.RoleRepository;
import com.bor.eboard.identity.repository.SectionRepository;
import com.bor.eboard.identity.repository.UserRepository;
import com.bor.eboard.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final DesignationRepository designationRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<SectionRef> findSection(UUID sectionId) {
        return sectionRepository.findByIdAndDeletedFalse(sectionId)
                .map(section -> new SectionRef(
                        section.getId(), section.getDepartmentId(), section.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRef> findUser(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .map(this::toUserRef);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeRef> findEmployee(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId).map(this::toEmployeeRef);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeRef> employees() {
        return userRepository.findAll().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .map(this::toEmployeeRef)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean departmentExists(UUID departmentId) {
        return departmentRepository.findByIdAndDeletedFalse(departmentId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean roleExists(UUID roleId) {
        return roleRepository.findByIdAndDeletedFalse(roleId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean designationExists(UUID designationId) {
        return designationRepository.findByIdAndDeletedFalse(designationId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> departmentNames() {
        return departmentRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> sectionNames() {
        return sectionRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Section::getId, Section::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> activeDepartmentNames() {
        return departmentRepository.findByDeletedFalseOrderByNameAsc().stream()
                .filter(department -> Boolean.TRUE.equals(department.getActive()))
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionRef> activeSections(UUID departmentId) {
        List<Section> sections = departmentId == null
                ? sectionRepository.findByDeletedFalseOrderByNameAsc()
                : sectionRepository.findByDepartmentIdAndDeletedFalseOrderByNameAsc(departmentId);
        return sections.stream()
                .filter(section -> Boolean.TRUE.equals(section.getActive()))
                .map(section -> new SectionRef(
                        section.getId(), section.getDepartmentId(), section.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRef> searchActiveUsers(String query, UUID departmentId,
                                           UUID sectionId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        String normalized = query == null || query.isBlank()
                ? null
                : "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        return userRepository.search(
                        normalized, sectionId, departmentId, STATUS_ACTIVE,
                        org.springframework.data.domain.PageRequest.of(0, safeLimit,
                                org.springframework.data.domain.Sort.by("fullName").ascending()))
                .getContent().stream()
                .map(this::toUserRef)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> userNames() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> roleNames() {
        return roleRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));
    }

    // ---------------------------------------------------------------------
    // BCR-03 (Part 5): assignment-rule support. Additive only.
    // ---------------------------------------------------------------------

    @Override
    public List<UserRef> activeUsersByRole(UUID roleId, UUID sectionId) {
        if (roleId == null) {
            return List.of();
        }
        return userRepository.findActiveByRoleAndOptionalSection(roleId, sectionId).stream()
                .map(this::toUserRef)
                .toList();
    }

    @Override
    public Map<UUID, String> roleCodes() {
        return roleRepository.findByDeletedFalseOrderByNameAsc().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));
    }

    @Override
    public Map<UUID, String> designationNames() {
        return designationRepository.findByDeletedFalseOrderByHierarchyLevelAsc().stream()
                .collect(Collectors.toMap(
                        com.bor.eboard.identity.entity.Designation::getId,
                        com.bor.eboard.identity.entity.Designation::getName));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateUserPosting(UUID userId, UUID departmentId, UUID sectionId,
                                  UUID designationId, String district) {
        userRepository.findByIdAndDeletedFalse(userId).ifPresent(user -> {
            if (departmentId != null) {
                user.setDepartmentId(departmentId);
            }
            if (sectionId != null) {
                user.setSectionId(sectionId);
            }
            if (designationId != null) {
                user.setDesignationId(designationId);
            }
            if (district != null && !district.isBlank()) {
                user.setDistrict(district.trim());
            }
            userRepository.save(user);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateUserStatus(UUID userId, String status) {
        userRepository.findByIdAndDeletedFalse(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> activeRoleIdsOf(UUID userId) {
        LocalDate today = LocalDate.now();
        return userRoleRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(ur -> isEffective(ur, today))
                .map(UserRole::getRoleId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> resolveStepAssignee(UUID specificUserId, UUID roleId,
                                              UUID designationId, UUID sectionId) {
        // 1. A named user always wins.
        if (specificUserId != null) {
            return userRepository.findByIdAndDeletedFalse(specificUserId)
                    .filter(u -> STATUS_ACTIVE.equals(u.getStatus()))
                    .map(User::getId);
        }
        // 2. First active user holding the role (optionally within the section).
        if (roleId != null) {
            LocalDate today = LocalDate.now();
            List<UUID> userIds = userRoleRepository.findByRoleIdAndActiveTrue(roleId).stream()
                    .filter(ur -> isEffective(ur, today))
                    .map(UserRole::getUserId)
                    .distinct()
                    .toList();
            Optional<User> match = userRepository.findByIdInAndDeletedFalse(userIds).stream()
                    .filter(u -> STATUS_ACTIVE.equals(u.getStatus()))
                    .filter(u -> sectionId == null || sectionId.equals(u.getSectionId()))
                    .findFirst();
            if (match.isPresent()) {
                return match.map(User::getId);
            }
        }
        // 3. First active user in the section (by designation if provided).
        if (sectionId != null) {
            return userRepository
                    .findBySectionIdAndDeletedFalseAndStatus(sectionId, STATUS_ACTIVE).stream()
                    .filter(u -> designationId == null
                            || designationId.equals(u.getDesignationId()))
                    .map(User::getId)
                    .findFirst();
        }
        // 4. Nothing concrete: leave to role/section targeting on the task.
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userMatchesAssignment(UUID userId, UUID assignedUserId,
                                         UUID assignedRoleId, UUID assignedSectionId) {
        if (userId == null) {
            return false;
        }
        if (assignedUserId != null && assignedUserId.equals(userId)) {
            return true;
        }
        if (assignedRoleId != null && activeRoleIdsOf(userId).contains(assignedRoleId)) {
            return true;
        }
        if (assignedSectionId != null) {
            return userRepository.findByIdAndDeletedFalse(userId)
                    .map(u -> assignedSectionId.equals(u.getSectionId()))
                    .orElse(false);
        }
        return false;
    }

    // ------------------------------------------------------------------

    private EmployeeRef toEmployeeRef(User user) {
        return new EmployeeRef(user.getId(), user.getEmployeeCode(), user.getFullName(),
                user.getEmail(), user.getMobile(), user.getStatus(), user.getDepartmentId(),
                user.getSectionId(), user.getDesignationId(), user.getDateOfBirth(),
                user.getDateOfJoining(), user.getProfileAttachmentId(), user.getDistrict());
    }

    private UserRef toUserRef(User user) {
        return new UserRef(user.getId(), user.getSectionId(), user.getDepartmentId(),
                user.getDesignationId(), user.getFullName(),
                STATUS_ACTIVE.equals(user.getStatus()));
    }

    private boolean isEffective(UserRole userRole, LocalDate today) {
        boolean startsOk = userRole.getEffectiveFrom() == null
                || !today.isBefore(userRole.getEffectiveFrom());
        boolean endsOk = userRole.getEffectiveTo() == null
                || !today.isAfter(userRole.getEffectiveTo());
        return startsOk && endsOk;
    }
}
