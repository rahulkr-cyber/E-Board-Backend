package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsAuditEventResponse;
import com.bor.eboard.dms.dto.DmsAuditSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentHistoryResponse;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentAudit;
import com.bor.eboard.dms.entity.DmsDocumentHistory;
import com.bor.eboard.dms.repository.DmsDocumentAuditRepository;
import com.bor.eboard.dms.repository.DmsDocumentHistoryRepository;
import com.bor.eboard.dms.repository.DmsDocumentRepository;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.service.DmsAuditQueryService;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DmsAuditQueryServiceImpl implements DmsAuditQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final DmsDocumentAuditRepository auditRepository;
    private final DmsDocumentHistoryRepository historyRepository;
    private final DmsDocumentRepository documentRepository;
    private final DmsDocumentAuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public DmsAuditQueryServiceImpl(
            DmsDocumentAuditRepository auditRepository,
            DmsDocumentHistoryRepository historyRepository,
            DmsDocumentRepository documentRepository,
            DmsDocumentAuthorizationService authorizationService,
            ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.historyRepository = historyRepository;
        this.documentRepository = documentRepository;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DmsAuditEventResponse> search(DmsAuditSearchRequest request) {
        DmsAuditSearchRequest filter = request == null
                ? new DmsAuditSearchRequest(null, null, null, null, null, null, 0, DEFAULT_SIZE)
                : request;
        validate(filter);
        int pageNumber = filter.page() == null ? 0 : filter.page();
        int pageSize = filter.size() == null ? DEFAULT_SIZE : filter.size();
        Page<DmsDocumentAudit> page = auditRepository.findAll(
                specification(filter),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(page, this::toAuditResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsAuditEventResponse> findDocumentAudit(UUID documentId) {
        requireDocument(documentId, false);
        DmsAuditSearchRequest request = new DmsAuditSearchRequest(
                documentId, null, null, null, null, null, 0, MAX_SIZE);
        return auditRepository.findAll(
                        specification(request),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .limit(MAX_SIZE)
                .map(this::toAuditResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsDocumentHistoryResponse> findDocumentHistory(UUID documentId) {
        requireDocument(documentId, true);
        return historyRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private Specification<DmsDocumentAudit> specification(DmsAuditSearchRequest request) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.documentId() != null) {
                predicates.add(builder.equal(root.get("documentId"), request.documentId()));
            }
            if (hasText(request.entityType())) {
                predicates.add(builder.equal(
                        builder.upper(root.get("entityType")),
                        request.entityType().trim().toUpperCase(Locale.ROOT)));
            }
            if (hasText(request.action())) {
                predicates.add(builder.equal(
                        builder.upper(root.get("action")),
                        request.action().trim().toUpperCase(Locale.ROOT)));
            }
            if (request.actorId() != null) {
                predicates.add(builder.equal(root.get("actorId"), request.actorId()));
            }
            if (request.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), request.from()));
            }
            if (request.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), request.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validate(DmsAuditSearchRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        if (page < 0) {
            throw new ValidationException("DMS audit page cannot be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("DMS audit page size must be between 1 and " + MAX_SIZE);
        }
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new ValidationException("DMS audit start date cannot be after end date");
        }
    }

    private DmsDocument requireDocument(UUID documentId, boolean requireView) {
        DmsDocument document = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document", documentId));
        if (requireView) {
            authorizationService.require(document, DmsDocumentAccessLevel.VIEW);
        }
        return document;
    }

    private DmsAuditEventResponse toAuditResponse(DmsDocumentAudit value) {
        return new DmsAuditEventResponse(
                value.getId(),
                value.getDocumentId(),
                value.getEntityType(),
                value.getEntityId(),
                value.getAction(),
                value.getOldValue(),
                value.getNewValue(),
                value.getActorId(),
                value.getActorName(),
                value.getIpAddress(),
                value.getApiPath(),
                value.getHttpMethod(),
                Boolean.TRUE.equals(value.getSuccess()),
                value.getErrorMessage(),
                value.getCreatedAt());
    }

    private DmsDocumentHistoryResponse toHistoryResponse(DmsDocumentHistory value) {
        return new DmsDocumentHistoryResponse(
                value.getId(),
                value.getDocumentId(),
                value.getEventType(),
                value.getEntityType(),
                value.getEntityId(),
                value.getVersionNumber(),
                value.getSummary(),
                readSnapshot(value.getSnapshotJson()),
                value.getActorId(),
                value.getActorName(),
                value.getCreatedAt());
    }

    private Map<String, Object> readSnapshot(String value) {
        if (!hasText(value)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ex) {
            return Map.of("raw", value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
