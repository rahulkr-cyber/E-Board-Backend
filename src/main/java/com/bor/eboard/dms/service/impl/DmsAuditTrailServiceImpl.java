package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.dms.audit.DmsAuditTrailEvent;
import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DmsAuditTrailServiceImpl implements DmsAuditTrailService {

    private static final Logger log = LoggerFactory.getLogger(DmsAuditTrailServiceImpl.class);

    private final DmsAuditTrailWriter writer;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public DmsAuditTrailServiceImpl(
            DmsAuditTrailWriter writer,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.writer = writer;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue) {
        schedule(newEvent(
                null, entityType, entityId, action, oldValue, newValue,
                null, null, null));
    }

    @Override
    public void recordDocumentAudit(
            UUID documentId,
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue) {
        schedule(newEvent(
                documentId, entityType, entityId, action, oldValue, newValue,
                null, null, null));
    }

    @Override
    public void recordDocumentHistory(
            UUID documentId,
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue,
            String summary,
            Integer versionNumber,
            Object snapshot) {
        schedule(newEvent(
                documentId, entityType, entityId, action, oldValue, newValue,
                summary, versionNumber, toJson(snapshot)));
    }

    private DmsAuditTrailEvent newEvent(
            UUID documentId,
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue,
            String historySummary,
            Integer versionNumber,
            String snapshotJson) {
        UUID actorId = SecurityUtils.getCurrentUserId().orElse(null);
        String actorName = actorId == null ? null : userRepository
                .findByIdAndDeletedFalse(actorId)
                .map(User::getFullName)
                .orElse(null);
        RequestDetails request = currentRequest();
        return new DmsAuditTrailEvent(
                documentId,
                normalize(entityType, "UNKNOWN"),
                entityId,
                normalize(action, "UNKNOWN"),
                oldValue,
                newValue,
                actorId,
                actorName,
                request.ipAddress(),
                request.apiPath(),
                request.httpMethod(),
                true,
                null,
                historySummary,
                versionNumber,
                snapshotJson,
                LocalDateTime.now());
    }

    private void schedule(DmsAuditTrailEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            writeSafely(event);
                        }
                    });
            return;
        }
        writeSafely(event);
    }

    private void writeSafely(DmsAuditTrailEvent event) {
        try {
            writer.write(event);
        } catch (Exception ex) {
            log.error("Failed to write DMS audit trail for action {}", event.action(), ex);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize DMS history snapshot", ex);
            return null;
        }
    }

    private RequestDetails currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new RequestDetails(null, null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ipAddress = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        return new RequestDetails(ipAddress, request.getRequestURI(), request.getMethod());
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record RequestDetails(
            String ipAddress,
            String apiPath,
            String httpMethod) {
    }
}
