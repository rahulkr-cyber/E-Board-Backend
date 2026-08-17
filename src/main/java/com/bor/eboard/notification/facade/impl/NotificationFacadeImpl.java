package com.bor.eboard.notification.facade.impl;

import com.bor.eboard.notification.entity.Notification;
import com.bor.eboard.notification.entity.NotificationTemplate;
import com.bor.eboard.notification.facade.NotificationFacade;
import com.bor.eboard.notification.repository.NotificationRepository;
import com.bor.eboard.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Notification facade implementation. Raising a notification participates in
 * the caller's transaction (REQUIRED) so an action and its notification
 * commit together; if the caller rolls back, the notification does too.
 */
@Component
@RequiredArgsConstructor
public class NotificationFacadeImpl implements NotificationFacade {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final com.bor.eboard.notification.repository.TaskEscalationRepository taskEscalationRepository;
    private final com.bor.eboard.notification.repository.EscalationRuleRepository escalationRuleRepository;

    @Override
    @Transactional
    public void notifyFromTemplate(UUID userId, String templateCode,
                                   Map<String, String> values,
                                   String linkedEntityType, UUID linkedEntityId,
                                   String priority) {
        if (userId == null) {
            return;
        }
        NotificationTemplate template = templateRepository
                .findByCodeAndDeletedFalseAndActiveTrue(templateCode).orElse(null);
        String title;
        String message;
        String type;
        if (template != null) {
            title = render(template.getTitleTemplate(), values);
            message = render(template.getMessageTemplate(), values);
            type = template.getNotificationType();
        } else {
            // Fall back to a generic message so a missing template never
            // silently loses an important alert.
            title = templateCode;
            message = values != null ? values.toString() : templateCode;
            type = templateCode;
        }
        persist(userId, title, message, type, linkedEntityType, linkedEntityId, priority);
    }

    @Override
    @Transactional
    public void notify(UUID userId, String title, String message, String notificationType,
                       String linkedEntityType, UUID linkedEntityId, String priority) {
        if (userId == null) {
            return;
        }
        persist(userId, title, message, notificationType, linkedEntityType, linkedEntityId, priority);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> latestEscalationLevel(UUID taskId) {
        if (taskId == null) {
            return java.util.Optional.empty();
        }
        return taskEscalationRepository.findFirstByTaskIdOrderByDaysAfterDueDesc(taskId)
                .map(com.bor.eboard.notification.entity.TaskEscalation::getEscalationLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<EscalationRuleView> activeEscalationRules() {
        return escalationRuleRepository.findByDeletedFalseAndActiveTrueOrderByDaysAfterDueAsc().stream()
                .map(rule -> new EscalationRuleView(rule.getCode(), rule.getName(),
                        rule.getDaysAfterDue(), rule.getEscalationLevel()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, EscalationState> latestEscalations(
            java.util.Collection<UUID> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<UUID, EscalationState> result = new java.util.HashMap<>();
        for (var item : taskEscalationRepository.findByTaskIdIn(taskIds)) {
            EscalationState candidate = new EscalationState(item.getEscalationLevel(),
                    item.getDaysAfterDue(), item.getEscalatedAt());
            EscalationState current = result.get(item.getTaskId());
            if (current == null || candidate.daysAfterDue() > current.daysAfterDue()) {
                result.put(item.getTaskId(), candidate);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<EscalationState> escalationHistory(UUID taskId) {
        if (taskId == null) {
            return java.util.List.of();
        }
        return taskEscalationRepository.findByTaskIdOrderByDaysAfterDueAsc(taskId).stream()
                .map(item -> new EscalationState(item.getEscalationLevel(),
                        item.getDaysAfterDue(), item.getEscalatedAt()))
                .toList();
    }

    private void persist(UUID userId, String title, String message, String type,
                         String linkedEntityType, UUID linkedEntityId, String priority) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(truncate(title, 255));
        notification.setMessage(message != null ? message : "");
        notification.setNotificationType(type != null ? type : "GENERAL");
        notification.setPriority(priority);
        notification.setLinkedEntityType(linkedEntityType);
        notification.setLinkedEntityId(linkedEntityId);
        notification.setReadFlag(Boolean.FALSE);
        notificationRepository.save(notification);
    }

    /** Replace {token} placeholders with supplied values. */
    private String render(String template, Map<String, String> values) {
        if (template == null) {
            return "";
        }
        String result = template;
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}",
                        entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return result;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
