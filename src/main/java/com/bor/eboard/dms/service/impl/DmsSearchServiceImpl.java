package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.document.DmsDocumentStatus;
import com.bor.eboard.dms.dto.DmsDocumentSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentSearchResponse;
import com.bor.eboard.dms.repository.DmsSearchQueryRepository;
import com.bor.eboard.dms.security.DmsDocumentAccessScope;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.bor.eboard.dms.service.DmsSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class DmsSearchServiceImpl implements DmsSearchService {

    private static final String ENTITY_TYPE = "SEARCH";

    private final DmsSearchQueryRepository searchRepository;
    private final DmsAuditTrailService auditTrailService;
    private final DmsDocumentAuthorizationService authorizationService;

    public DmsSearchServiceImpl(
            DmsSearchQueryRepository searchRepository,
            DmsAuditTrailService auditTrailService,
            DmsDocumentAuthorizationService authorizationService) {
        this.searchRepository = searchRepository;
        this.auditTrailService = auditTrailService;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DmsDocumentSearchResponse> search(DmsDocumentSearchRequest request) {
        validate(request);
        DmsDocumentAccessScope scope = authorizationService.currentScope();
        UUID currentUserId = scope.userId();
        PageResponse<DmsDocumentSearchResponse> result = searchRepository.search(request, scope);
        auditTrailService.record(ENTITY_TYPE, currentUserId, "SEARCH", null,
                "keywords=" + safe(request.keywords())
                        + ";documentTypeId=" + request.documentTypeId()
                        + ";status=" + safe(request.status())
                        + ";page=" + defaultPage(request.page())
                        + ";size=" + defaultSize(request.size())
                        + ";results=" + result.getTotalElements());
        return result;
    }

    private void validate(DmsDocumentSearchRequest request) {
        if (request == null) {
            throw new ValidationException("DMS search request is required");
        }
        if (request.uploadedFrom() != null && request.uploadedTo() != null
                && request.uploadedFrom().isAfter(request.uploadedTo())) {
            throw new ValidationException("DMS upload date start cannot be after end date");
        }
        if (request.modifiedFrom() != null && request.modifiedTo() != null
                && request.modifiedFrom().isAfter(request.modifiedTo())) {
            throw new ValidationException("DMS modified date start cannot be after end date");
        }
        if (request.minFileSizeBytes() != null && request.maxFileSizeBytes() != null
                && request.minFileSizeBytes() > request.maxFileSizeBytes()) {
            throw new ValidationException("DMS minimum file size cannot exceed maximum file size");
        }
        if (request.status() != null && !request.status().isBlank()) {
            try {
                DmsDocumentStatus.valueOf(request.status().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Unsupported DMS document status: " + request.status());
            }
        }
        String direction = request.sortDirection();
        if (direction != null && !direction.isBlank()
                && !"ASC".equalsIgnoreCase(direction)
                && !"DESC".equalsIgnoreCase(direction)) {
            throw new ValidationException("DMS search sort direction must be ASC or DESC");
        }
    }

    private int defaultPage(Integer value) {
        return value == null ? 0 : value;
    }

    private int defaultSize(Integer value) {
        return value == null ? 20 : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
