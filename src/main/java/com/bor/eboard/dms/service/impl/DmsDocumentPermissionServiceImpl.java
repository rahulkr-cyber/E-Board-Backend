package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsDocumentAccessGrantRequest;
import com.bor.eboard.dms.dto.DmsDocumentAccessResponse;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentPermission;
import com.bor.eboard.dms.repository.DmsDocumentPermissionRepository;
import com.bor.eboard.dms.repository.DmsDocumentRepository;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.service.DmsAccessPrincipalService;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.bor.eboard.dms.service.DmsDocumentPermissionService;
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
public class DmsDocumentPermissionServiceImpl implements DmsDocumentPermissionService {

    private static final String ENTITY_TYPE = "DOCUMENT_PERMISSION";

    private final DmsDocumentRepository documentRepository;
    private final DmsDocumentPermissionRepository permissionRepository;
    private final DmsDocumentAuthorizationService authorizationService;
    private final DmsAccessPrincipalService principalService;
    private final DmsDocumentAccessResponseFactory responseFactory;
    private final DmsAuditTrailService auditTrailService;

    public DmsDocumentPermissionServiceImpl(
            DmsDocumentRepository documentRepository,
            DmsDocumentPermissionRepository permissionRepository,
            DmsDocumentAuthorizationService authorizationService,
            DmsAccessPrincipalService principalService,
            DmsDocumentAccessResponseFactory responseFactory,
            DmsAuditTrailService auditTrailService) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.authorizationService = authorizationService;
        this.principalService = principalService;
        this.responseFactory = responseFactory;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDocumentAccessResponse getAccess(UUID documentId) {
        DmsDocument document = findDocument(documentId);
        authorizationService.require(document, DmsDocumentAccessLevel.SHARE);
        return responseFactory.create(document);
    }

    @Override
    @Transactional
    public DmsDocumentAccessResponse grant(
            UUID documentId,
            DmsDocumentAccessGrantRequest request) {
        DmsDocument document = findDocument(documentId);
        authorizationService.require(document, DmsDocumentAccessLevel.SHARE);
        principalService.requireName(request.principalType(), request.principalId());
        validateExpiry(request.expiresAt());
        Set<DmsDocumentAccessLevel> levels = normalizeLevels(request.accessLevels());
        validateDelegation(document, levels);

        List<DmsDocumentPermission> existing = permissionRepository
                .findByDocumentIdAndPrincipalTypeAndPrincipalIdAndActiveTrueAndDeletedFalse(
                        documentId, request.principalType(), request.principalId());
        expireStale(existing);
        existing = existing.stream().filter(value -> Boolean.TRUE.equals(value.getActive())).toList();
        if (existing.stream().anyMatch(value -> value.getShareId() != null)) {
            throw new DuplicateResourceException(
                    "DMS access is already granted through an active share");
        }

        List<DmsDocumentPermission> changed = new ArrayList<>();
        for (DmsDocumentAccessLevel level : levels) {
            DmsDocumentPermission permission = permissionRepository
                    .findFirstByDocumentIdAndPrincipalTypeAndPrincipalIdAndAccessLevelAndShareIdIsNullAndDeletedFalseOrderByCreatedAtDesc(
                            documentId, request.principalType(), request.principalId(), level)
                    .orElseGet(DmsDocumentPermission::new);
            permission.setDocumentId(documentId);
            permission.setShareId(null);
            permission.setPrincipalType(request.principalType());
            permission.setPrincipalId(request.principalId());
            permission.setAccessLevel(level);
            permission.setExpiresAt(request.expiresAt());
            permission.setActive(Boolean.TRUE);
            permission.setDeleted(Boolean.FALSE);
            changed.add(permission);
        }
        permissionRepository.saveAll(changed);
        auditTrailService.recordDocumentHistory(
                documentId, ENTITY_TYPE, documentId,
                "PERMISSION_GRANT", null,
                request.principalType() + ":" + request.principalId()
                        + ";levels=" + levels + ";expiresAt=" + request.expiresAt(),
                "Document permissions granted", document.getCurrentVersionNumber(),
                accessSnapshot(request.principalType().name(), request.principalId(),
                        levels.toString(), request.expiresAt()));
        return responseFactory.create(document);
    }

    @Override
    @Transactional
    public DmsDocumentAccessResponse revoke(UUID documentId, UUID permissionId) {
        DmsDocument document = findDocument(documentId);
        authorizationService.require(document, DmsDocumentAccessLevel.SHARE);
        DmsDocumentPermission permission = permissionRepository
                .findByIdAndDocumentIdAndDeletedFalse(permissionId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DMS document permission", permissionId));
        permission.setActive(Boolean.FALSE);
        permissionRepository.save(permission);
        auditTrailService.recordDocumentHistory(
                documentId, ENTITY_TYPE, documentId,
                "PERMISSION_REVOKE",
                permission.getPrincipalType() + ":" + permission.getPrincipalId()
                        + ";level=" + permission.getAccessLevel(),
                null,
                "Document permission revoked", document.getCurrentVersionNumber(),
                accessSnapshot(permission.getPrincipalType().name(), permission.getPrincipalId(),
                        permission.getAccessLevel().name(), permission.getExpiresAt()));
        return responseFactory.create(document);
    }

    private Map<String, Object> accessSnapshot(
            String principalType,
            UUID principalId,
            String accessLevels,
            LocalDateTime expiresAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("principalType", principalType);
        snapshot.put("principalId", principalId);
        snapshot.put("accessLevels", accessLevels);
        snapshot.put("expiresAt", expiresAt);
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
            throw new ValidationException("DMS permission expiry must be in the future");
        }
    }

    private void expireStale(List<DmsDocumentPermission> permissions) {
        LocalDateTime now = LocalDateTime.now();
        List<DmsDocumentPermission> stale = permissions.stream()
                .filter(value -> value.getExpiresAt() != null
                        && !value.getExpiresAt().isAfter(now))
                .toList();
        if (!stale.isEmpty()) {
            stale.forEach(value -> value.setActive(Boolean.FALSE));
            permissionRepository.saveAll(stale);
        }
    }
}
