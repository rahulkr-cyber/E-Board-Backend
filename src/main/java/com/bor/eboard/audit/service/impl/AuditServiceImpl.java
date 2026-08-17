package com.bor.eboard.audit.service.impl;

import com.bor.eboard.audit.entity.AuditLog;
import com.bor.eboard.audit.repository.AuditLogRepository;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.constants.AppConstants;
import com.bor.eboard.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * REQUIRES_NEW so an audit failure never rolls back the business
     * transaction and vice versa.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String module, String entityType, UUID entityId,
                       String action, String oldValue, String newValue) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(SecurityUtils.getCurrentUserId().orElse(null));
            auditLog.setModule(module);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setSuccess(Boolean.TRUE);
            enrichWithRequest(auditLog);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to record audit log for action {}", action, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String module, String entityType, UUID entityId,
                              String action, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(SecurityUtils.getCurrentUserId().orElse(null));
            auditLog.setModule(module);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setSuccess(Boolean.FALSE);
            auditLog.setErrorMessage(errorMessage);
            enrichWithRequest(auditLog);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to record failed audit action {}", action, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuth(UUID userId, String username, String action,
                           boolean success, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setModule(AppConstants.MODULE_AUTH);
            auditLog.setEntityType("USER");
            auditLog.setEntityId(userId);
            auditLog.setAction(action);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMessage);
            enrichWithRequest(auditLog);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to record auth audit log for action {}", action, e);
        }
    }

    private void enrichWithRequest(AuditLog auditLog) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        auditLog.setIpAddress(forwarded != null
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr());
        auditLog.setMachineName(request.getRemoteHost());
        auditLog.setBrowser(request.getHeader("User-Agent"));
        auditLog.setApiPath(request.getRequestURI());
        auditLog.setHttpMethod(request.getMethod());
    }
}
