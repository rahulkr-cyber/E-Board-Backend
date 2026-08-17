package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsDocumentAccessSummaryResponse;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentPermission;
import com.bor.eboard.dms.repository.DmsDocumentPermissionRepository;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsDocumentAccessScope;
import com.bor.eboard.dms.security.DmsPrincipalType;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.entity.UserRole;
import com.bor.eboard.identity.repository.UserRepository;
import com.bor.eboard.identity.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DmsDocumentAuthorizationServiceImpl
        implements DmsDocumentAuthorizationService {

    private final DmsDocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public DmsDocumentAuthorizationServiceImpl(
            DmsDocumentPermissionRepository permissionRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDocumentAccessScope currentScope() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authenticated user is required"));
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user was not found"));
        LocalDate today = LocalDate.now();
        Set<UUID> roleIds = userRoleRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(role -> isEffective(role, today))
                .map(UserRole::getRoleId)
                .collect(Collectors.toUnmodifiableSet());
        return new DmsDocumentAccessScope(
                userId,
                roleIds,
                user.getDepartmentId(),
                user.getSectionId(),
                SecurityUtils.hasAuthority(DmsPermissions.ADMIN));
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDocumentAccessSummaryResponse summarize(DmsDocument document) {
        DmsDocumentAccessScope scope = currentScope();
        boolean owner = Objects.equals(document.getUploadedBy(), scope.userId());
        if (owner || scope.administrator()) {
            return new DmsDocumentAccessSummaryResponse(
                    owner,
                    scope.administrator(),
                    Set.copyOf(EnumSet.allOf(DmsDocumentAccessLevel.class)));
        }

        LocalDateTime now = LocalDateTime.now();
        Set<DmsDocumentAccessLevel> effective = permissionRepository
                .findByDocumentIdAndActiveTrueAndDeletedFalseOrderByCreatedAtAsc(document.getId())
                .stream()
                .filter(permission -> permission.getExpiresAt() == null
                        || permission.getExpiresAt().isAfter(now))
                .filter(permission -> matches(permission, scope))
                .map(DmsDocumentPermission::getAccessLevel)
                .collect(Collectors.toUnmodifiableSet());
        return new DmsDocumentAccessSummaryResponse(false, false, effective);
    }

    @Override
    @Transactional(readOnly = true)
    public void require(DmsDocument document, DmsDocumentAccessLevel accessLevel) {
        if (summarize(document).effectiveAccessLevels().contains(accessLevel)) {
            return;
        }
        throw new ForbiddenException("You do not have access to this DMS document");
    }

    private boolean matches(
            DmsDocumentPermission permission,
            DmsDocumentAccessScope scope) {
        DmsPrincipalType type = permission.getPrincipalType();
        UUID principalId = permission.getPrincipalId();
        return switch (type) {
            case USER -> Objects.equals(scope.userId(), principalId);
            case ROLE -> scope.roleIds().contains(principalId);
            case DEPARTMENT -> Objects.equals(scope.departmentId(), principalId);
            case SECTION -> Objects.equals(scope.sectionId(), principalId);
        };
    }

    private boolean isEffective(UserRole role, LocalDate today) {
        return (role.getEffectiveFrom() == null || !role.getEffectiveFrom().isAfter(today))
                && (role.getEffectiveTo() == null || !role.getEffectiveTo().isBefore(today));
    }
}
