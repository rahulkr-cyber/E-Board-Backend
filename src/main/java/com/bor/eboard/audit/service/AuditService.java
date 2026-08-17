package com.bor.eboard.audit.service;

import java.util.UUID;

/**
 * Records immutable audit events for important actions
 * (02_ARCHITECTURE.md section 17, 07_CLAUDE.md section 14).
 */
public interface AuditService {

    void record(String module, String entityType, UUID entityId,
                String action, String oldValue, String newValue);

    void recordFailure(String module, String entityType, UUID entityId,
                       String action, String errorMessage);

    void recordAuth(UUID userId, String username, String action,
                    boolean success, String errorMessage);
}
