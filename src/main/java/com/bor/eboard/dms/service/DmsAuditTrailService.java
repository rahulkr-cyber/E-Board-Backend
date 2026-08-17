package com.bor.eboard.dms.service;

import java.util.UUID;

public interface DmsAuditTrailService {

    void record(
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue);

    void recordDocumentAudit(
            UUID documentId,
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue);

    void recordDocumentHistory(
            UUID documentId,
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue,
            String summary,
            Integer versionNumber,
            Object snapshot);
}
