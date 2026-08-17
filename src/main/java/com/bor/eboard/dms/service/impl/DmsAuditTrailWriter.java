package com.bor.eboard.dms.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.dms.audit.DmsAuditTrailEvent;
import com.bor.eboard.dms.entity.DmsDocumentAudit;
import com.bor.eboard.dms.entity.DmsDocumentHistory;
import com.bor.eboard.dms.repository.DmsDocumentAuditRepository;
import com.bor.eboard.dms.repository.DmsDocumentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DmsAuditTrailWriter {

    private static final String MODULE = "DMS";

    private final DmsDocumentAuditRepository auditRepository;
    private final DmsDocumentHistoryRepository historyRepository;
    private final AuditService auditService;

    public DmsAuditTrailWriter(
            DmsDocumentAuditRepository auditRepository,
            DmsDocumentHistoryRepository historyRepository,
            AuditService auditService) {
        this.auditRepository = auditRepository;
        this.historyRepository = historyRepository;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(DmsAuditTrailEvent event) {
        DmsDocumentAudit audit = new DmsDocumentAudit();
        audit.setDocumentId(event.documentId());
        audit.setEntityType(event.entityType());
        audit.setEntityId(event.entityId());
        audit.setAction(event.action());
        audit.setOldValue(event.oldValue());
        audit.setNewValue(event.newValue());
        audit.setActorId(event.actorId());
        audit.setActorName(event.actorName());
        audit.setIpAddress(event.ipAddress());
        audit.setApiPath(event.apiPath());
        audit.setHttpMethod(event.httpMethod());
        audit.setSuccess(event.success());
        audit.setErrorMessage(event.errorMessage());
        audit.setCreatedAt(event.createdAt());
        auditRepository.save(audit);

        if (event.documentId() != null && event.historySummary() != null) {
            DmsDocumentHistory history = new DmsDocumentHistory();
            history.setDocumentId(event.documentId());
            history.setEventType(event.action());
            history.setEntityType(event.entityType());
            history.setEntityId(event.entityId());
            history.setVersionNumber(event.versionNumber());
            history.setSummary(event.historySummary());
            history.setSnapshotJson(event.snapshotJson());
            history.setActorId(event.actorId());
            history.setActorName(event.actorName());
            history.setCreatedAt(event.createdAt());
            historyRepository.save(history);
        }

        auditService.record(
                MODULE,
                event.entityType(),
                event.entityId(),
                event.action(),
                event.oldValue(),
                event.newValue());
    }
}
