package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.dms.dto.DmsDocumentAccessResponse;
import com.bor.eboard.dms.dto.DmsDocumentShareRequest;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentPermission;
import com.bor.eboard.dms.entity.DmsDocumentShare;
import com.bor.eboard.dms.repository.DmsDocumentPermissionRepository;
import com.bor.eboard.dms.repository.DmsDocumentRepository;
import com.bor.eboard.dms.repository.DmsDocumentShareRepository;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.service.DmsAccessPrincipalService;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.bor.eboard.dms.service.DmsDocumentShareService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DmsDocumentShareServiceImpl implements DmsDocumentShareService {

    private static final String ENTITY_TYPE = "DOCUMENT_SHARE";

    private final DmsDocumentRepository documentRepository;
    private final DmsDocumentShareRepository shareRepository;
    private final DmsDocumentPermissionRepository permissionRepository;
    private final DmsDocumentAuthorizationService authorizationService;
    private final DmsAccessPrincipalService principalService;
    private final DmsDocumentAccessResponseFactory responseFactory;
    private final DmsAuditTrailService auditTrailService;

    public DmsDocumentShareServiceImpl(
            DmsDocumentRepository documentRepository,
            DmsDocumentShareRepository shareRepository,
            DmsDocumentPermissionRepository permissionRepository,
            DmsDocumentAuthorizationService authorizationService,
            DmsAccessPrincipalService principalService,
            DmsDocumentAccessResponseFactory responseFactory,
            DmsAuditTrailService auditTrailService) {
        this.documentRepository = documentRepository;
        this.shareRepository = shareRepository;
        this.permissionRepository = permissionRepository;
        this.authorizationService = authorizationService;
        this.principalService = principalService;
        this.responseFactory = responseFactory;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional
    public DmsDocumentAccessResponse share(
            UUID documentId,
            DmsDocumentShareRequest request) {
        DmsDocument document = findDocument(documentId);
        authorizationService.require(document, DmsDocumentAccessLevel.SHARE);
        principalService.requireName(request.principalType(), request.principalId());
        validateExpiry(request.expiresAt());
        Set<DmsDocumentAccessLevel> levels = normalizeLevels(request.accessLevels());
        validateDelegation(document, levels);

        expireStaleShare(documentId, request);
        if (shareRepository
                .findFirstByDocumentIdAndPrincipalTypeAndPrincipalIdAndActiveTrueAndDeletedFalse(
                        documentId, request.principalType(), request.principalId())
                .isPresent()) {
            throw new DuplicateResourceException(
                    "The DMS document is already shared with this principal");
        }
        List<DmsDocumentPermission> existingPermissions = permissionRepository
                .findByDocumentIdAndPrincipalTypeAndPrincipalIdAndActiveTrueAndDeletedFalse(
                        documentId, request.principalType(), request.principalId());
        expireStalePermissions(existingPermissions);
        if (existingPermissions.stream().anyMatch(value -> Boolean.TRUE.equals(value.getActive()))) {
            throw new DuplicateResourceException(
                    "Direct DMS permissions already exist for this principal");
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authenticated user is required"));
        LocalDateTime now = LocalDateTime.now();
        DmsDocumentShare share = new DmsDocumentShare();
        share.setDocumentId(documentId);
        share.setPrincipalType(request.principalType());
        share.setPrincipalId(request.principalId());
        share.setSharedBy(currentUserId);
        share.setSharedAt(now);
        share.setExpiresAt(request.expiresAt());
        share.setShareNote(normalizeNote(request.note()));
        share.setActive(Boolean.TRUE);
        share = shareRepository.saveAndFlush(share);

        List<DmsDocumentPermission> permissions = new ArrayList<>();
        for (DmsDocumentAccessLevel level : levels) {
            DmsDocumentPermission permission = new DmsDocumentPermission();
            permission.setDocumentId(documentId);
            permission.setShareId(share.getId());
            permission.setPrincipalType(request.principalType());
            permission.setPrincipalId(request.principalId());
            permission.setAccessLevel(level);
            permission.setExpiresAt(request.expiresAt());
            permission.setActive(Boolean.TRUE);
            permissions.add(permission);
        }
        permissionRepository.saveAll(permissions);
        auditTrailService.recordDocumentHistory(
                documentId, ENTITY_TYPE, documentId,
                "SHARE", null,
                request.principalType() + ":" + request.principalId()
                        + ";levels=" + levels + ";expiresAt=" + request.expiresAt(),
                "Document shared", document.getCurrentVersionNumber(),
                shareSnapshot(request.principalType().name(), request.principalId(),
                        levels.toString(), request.expiresAt(), request.note()));
        return responseFactory.create(document);
    }

    @Override
    @Transactional
    public DmsDocumentAccessResponse revoke(UUID documentId, UUID shareId) {
        DmsDocument document = findDocument(documentId);
        authorizationService.require(document, DmsDocumentAccessLevel.SHARE);
        DmsDocumentShare share = shareRepository
                .findByIdAndDocumentIdAndDeletedFalse(shareId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document share", shareId));
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authenticated user is required"));
        share.setActive(Boolean.FALSE);
        share.setRevokedAt(LocalDateTime.now());
        share.setRevokedBy(currentUserId);
        shareRepository.save(share);

        List<DmsDocumentPermission> permissions = permissionRepository
                .findByShareIdAndDeletedFalse(shareId);
        permissions.forEach(value -> value.setActive(Boolean.FALSE));
        if (!permissions.isEmpty()) {
            permissionRepository.saveAll(permissions);
        }
        auditTrailService.recordDocumentHistory(
                documentId, ENTITY_TYPE, documentId,
                "SHARE_REVOKE",
                share.getPrincipalType() + ":" + share.getPrincipalId(),
                null,
                "Document share revoked", document.getCurrentVersionNumber(),
                shareSnapshot(share.getPrincipalType().name(), share.getPrincipalId(),
                        "REVOKED", share.getExpiresAt(), share.getShareNote()));
        return responseFactory.create(document);
    }

    private Map<String, Object> shareSnapshot(
            String principalType,
            UUID principalId,
            String accessLevels,
            LocalDateTime expiresAt,
            String note) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("principalType", principalType);
        snapshot.put("principalId", principalId);
        snapshot.put("accessLevels", accessLevels);
        snapshot.put("expiresAt", expiresAt);
        snapshot.put("note", note);
        return snapshot;
    }

    private DmsDocument findDocument(UUID documentId) {
        return documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document", documentId));
    }

    private Set<DmsDocumentAccessLevel> normalizeLevels(
            Set<DmsDocumentAccessLevel> requested) {
        if (requested == null || requested.isEmpty() || requested.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ValidationException("At least one valid DMS access level is required");
        }
        EnumSet<DmsDocumentAccessLevel> levels = EnumSet.copyOf(requested);
        levels.add(DmsDocumentAccessLevel.VIEW);
        return Set.copyOf(levels);
    }

    private void validateDelegation(
            DmsDocument document,
            Set<DmsDocumentAccessLevel> levels) {
        var currentAccess = authorizationService.summarize(document);
        if (currentAccess.owner() || currentAccess.administrator()
                || currentAccess.effectiveAccessLevels().containsAll(levels)) {
            return;
        }
        throw new ForbiddenException(
                "You cannot grant DMS access levels that you do not possess");
    }

    private void validateExpiry(LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new ValidationException("DMS share expiry must be in the future");
        }
    }

    private String normalizeNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void expireStaleShare(UUID documentId, DmsDocumentShareRequest request) {
        shareRepository
                .findFirstByDocumentIdAndPrincipalTypeAndPrincipalIdAndActiveTrueAndDeletedFalse(
                        documentId, request.principalType(), request.principalId())
                .filter(value -> value.getExpiresAt() != null
                        && !value.getExpiresAt().isAfter(LocalDateTime.now()))
                .ifPresent(value -> {
                    value.setActive(Boolean.FALSE);
                    shareRepository.save(value);
                    List<DmsDocumentPermission> permissions = permissionRepository
                            .findByShareIdAndDeletedFalse(value.getId());
                    permissions.forEach(permission -> permission.setActive(Boolean.FALSE));
                    permissionRepository.saveAll(permissions);
                });
    }

    private void expireStalePermissions(List<DmsDocumentPermission> permissions) {
        LocalDateTime now = LocalDateTime.now();
        List<DmsDocumentPermission> stale = permissions.stream()
                .filter(value -> value.getExpiresAt() != null
                        && !value.getExpiresAt().isAfter(now))
                .toList();
        stale.forEach(value -> value.setActive(Boolean.FALSE));
        if (!stale.isEmpty()) {
            permissionRepository.saveAll(stale);
        }
    }
}
