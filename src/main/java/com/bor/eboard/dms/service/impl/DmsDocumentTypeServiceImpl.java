package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.dms.dto.DmsDocumentTypeCreateRequest;
import com.bor.eboard.dms.dto.DmsDocumentTypeResponse;
import com.bor.eboard.dms.dto.DmsDocumentTypeUpdateRequest;
import com.bor.eboard.dms.entity.DmsDocumentType;
import com.bor.eboard.dms.repository.DmsDocumentTypeRepository;
import com.bor.eboard.dms.service.DmsDocumentTypeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DmsDocumentTypeServiceImpl implements DmsDocumentTypeService {

    private static final String ENTITY_TYPE = "DOCUMENT_TYPE";

    private final DmsDocumentTypeRepository repository;
    private final DmsAuditTrailService auditTrailService;

    public DmsDocumentTypeServiceImpl(
            DmsDocumentTypeRepository repository,
            DmsAuditTrailService auditTrailService) {
        this.repository = repository;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsDocumentTypeResponse> findAll(boolean activeOnly) {
        List<DmsDocumentType> documentTypes = activeOnly
                ? repository.findByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc()
                : repository.findByDeletedFalseOrderBySortOrderAscNameAsc();
        return documentTypes.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDocumentTypeResponse findById(UUID id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public DmsDocumentTypeResponse create(DmsDocumentTypeCreateRequest request) {
        String code = normalizeCode(request.code());
        String name = normalizeRequiredText(request.name());
        validateUniqueForCreate(code, name);

        DmsDocumentType entity = new DmsDocumentType();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(normalizeOptionalText(request.description()));
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        entity.setActive(request.active() == null ? Boolean.TRUE : request.active());

        entity = save(entity);
        auditTrailService.record(ENTITY_TYPE, entity.getId(),
                "CREATE", null, auditValue(entity));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public DmsDocumentTypeResponse update(UUID id, DmsDocumentTypeUpdateRequest request) {
        DmsDocumentType entity = findEntity(id);
        String name = normalizeRequiredText(request.name());
        if (repository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse(name, id)) {
            throw new DuplicateResourceException("DMS document type name already exists: " + name);
        }

        String oldValue = auditValue(entity);
        entity.setName(name);
        entity.setDescription(normalizeOptionalText(request.description()));
        entity.setSortOrder(request.sortOrder() == null ? entity.getSortOrder() : request.sortOrder());
        if (request.active() != null) {
            entity.setActive(request.active());
        }

        entity = save(entity);
        auditTrailService.record(ENTITY_TYPE, entity.getId(),
                "UPDATE", oldValue, auditValue(entity));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public DmsDocumentTypeResponse setActive(UUID id, boolean active) {
        DmsDocumentType entity = findEntity(id);
        String oldValue = auditValue(entity);
        entity.setActive(active);
        entity = repository.save(entity);
        auditTrailService.record(ENTITY_TYPE, entity.getId(),
                active ? "ACTIVATE" : "DEACTIVATE", oldValue, auditValue(entity));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DmsDocumentType entity = findEntity(id);
        String oldValue = auditValue(entity);
        entity.setDeleted(Boolean.TRUE);
        repository.save(entity);
        auditTrailService.record(ENTITY_TYPE, entity.getId(),
                "DELETE", oldValue, "soft-deleted");
    }

    private void validateUniqueForCreate(String code, String name) {
        if (repository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
            throw new DuplicateResourceException("DMS document type code already exists: " + code);
        }
        if (repository.existsByNameIgnoreCaseAndDeletedFalse(name)) {
            throw new DuplicateResourceException("DMS document type name already exists: " + name);
        }
    }

    private DmsDocumentType save(DmsDocumentType entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "A DMS document type with the same code or name already exists");
        }
    }

    private DmsDocumentType findEntity(UUID id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document type", id));
    }

    private DmsDocumentTypeResponse toResponse(DmsDocumentType entity) {
        return new DmsDocumentTypeResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String auditValue(DmsDocumentType entity) {
        return "code=" + entity.getCode()
                + ", name=" + entity.getName()
                + ", sortOrder=" + entity.getSortOrder()
                + ", active=" + entity.getActive();
    }
}
