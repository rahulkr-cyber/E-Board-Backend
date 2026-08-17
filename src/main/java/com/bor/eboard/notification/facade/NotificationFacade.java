package com.bor.eboard.notification.facade;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cross-module boundary for raising notifications and reading SLA escalation configuration/history. */
public interface NotificationFacade {

    void notifyFromTemplate(UUID userId, String templateCode,
                            Map<String, String> values,
                            String linkedEntityType, UUID linkedEntityId,
                            String priority);

    void notify(UUID userId, String title, String message, String notificationType,
                String linkedEntityType, UUID linkedEntityId, String priority);

    Optional<String> latestEscalationLevel(UUID taskId);

    record EscalationRuleView(String code, String name, int daysAfterDue,
                              String escalationLevel) { }

    record EscalationState(String escalationLevel, int daysAfterDue,
                           LocalDateTime escalatedAt) { }

    List<EscalationRuleView> activeEscalationRules();

    Map<UUID, EscalationState> latestEscalations(Collection<UUID> taskIds);

    List<EscalationState> escalationHistory(UUID taskId);
}
