package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.dto.DmsDocumentAccessResponse;
import com.bor.eboard.dms.dto.DmsDocumentPermissionResponse;
import com.bor.eboard.dms.dto.DmsDocumentShareResponse;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentPermission;
import com.bor.eboard.dms.entity.DmsDocumentShare;
import com.bor.eboard.dms.repository.DmsDocumentPermissionRepository;
import com.bor.eboard.dms.repository.DmsDocumentShareRepository;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.service.DmsAccessPrincipalService;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.bor.eboard.identity.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DmsDocumentAccessResponseFactory {

    private final DmsDocumentPermissionRepository permissionRepository;
    private final DmsDocumentShareRepository shareRepository;
    private final DmsDocumentAuthorizationService authorizationService;
    private final DmsAccessPrincipalService principalService;
    private final UserRepository userRepository;

    public DmsDocumentAccessResponseFactory(
            DmsDocumentPermissionRepository permissionRepository,
            DmsDocumentShareRepository shareRepository,
            DmsDocumentAuthorizationService authorizationService,
            DmsAccessPrincipalService principalService,
            UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.shareRepository = shareRepository;
        this.authorizationService = authorizationService;
        this.principalService = principalService;
        this.userRepository = userRepository;
    }

    public DmsDocumentAccessResponse create(DmsDocument document) {
        LocalDateTime now = LocalDateTime.now();
        List<DmsDocumentPermission> permissions = permissionRepository
                .findByDocumentIdAndActiveTrueAndDeletedFalseOrderByCreatedAtAsc(document.getId())
                .stream()
                .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(now))
                .toList();
        List<DmsDocumentShare> shares = shareRepository
                .findByDocumentIdAndActiveTrueAndDeletedFalseOrderBySharedAtDesc(document.getId())
                .stream()
                .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(now))
                .toList();

        return new DmsDocumentAccessResponse(
                document.getId(),
                document.getDocumentNumber(),
                authorizationService.summarize(document),
                permissions.stream().map(this::toPermission).toList(),
                shares.stream().map(share -> toShare(share, permissions)).toList());
    }

    private DmsDocumentPermissionResponse toPermission(DmsDocumentPermission value) {
        return new DmsDocumentPermissionResponse(
                value.getId(),
                value.getShareId(),
                value.getPrincipalType(),
                value.getPrincipalId(),
                safePrincipalName(value),
                value.getAccessLevel(),
                value.getExpiresAt(),
                Boolean.TRUE.equals(value.getActive()),
                value.getCreatedBy(),
                value.getCreatedAt());
    }

    private DmsDocumentShareResponse toShare(
            DmsDocumentShare value,
            List<DmsDocumentPermission> permissions) {
        Set<DmsDocumentAccessLevel> accessLevels = new LinkedHashSet<>();
        permissions.stream()
                .filter(permission -> value.getId().equals(permission.getShareId()))
                .map(DmsDocumentPermission::getAccessLevel)
                .forEach(accessLevels::add);
        return new DmsDocumentShareResponse(
                value.getId(),
                value.getPrincipalType(),
                value.getPrincipalId(),
                safePrincipalName(value),
                Set.copyOf(accessLevels),
                value.getSharedBy(),
                userRepository.findByIdAndDeletedFalse(value.getSharedBy())
                        .map(user -> user.getFullName())
                        .orElse(value.getSharedBy().toString()),
                value.getSharedAt(),
                value.getExpiresAt(),
                value.getShareNote(),
                Boolean.TRUE.equals(value.getActive()));
    }

    private String safePrincipalName(DmsDocumentPermission value) {
        try {
            return principalService.requireName(value.getPrincipalType(), value.getPrincipalId());
        } catch (RuntimeException ex) {
            return value.getPrincipalId().toString();
        }
    }

    private String safePrincipalName(DmsDocumentShare value) {
        try {
            return principalService.requireName(value.getPrincipalType(), value.getPrincipalId());
        } catch (RuntimeException ex) {
            return value.getPrincipalId().toString();
        }
    }
}
