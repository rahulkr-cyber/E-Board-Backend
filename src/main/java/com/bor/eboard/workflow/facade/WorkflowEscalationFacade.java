package com.bor.eboard.workflow.facade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module read boundary for SLA/escalation monitoring. Dashboard and
 * notification code consume workflow state through this facade and never
 * mutate task ownership.
 */
public interface WorkflowEscalationFacade {

    /** Read-only state of a pending task whose SLA due date has passed. */
    record OverdueTask(
            UUID taskId,
            UUID workflowInstanceId,
            UUID fileId,
            UUID letterId,
            UUID stepId,
            String stepName,
            UUID assignedToUserId,
            UUID assignedToRoleId,
            UUID assignedToSectionId,
            LocalDate dueDate,
            LocalDateTime assignedAt,
            LocalDateTime lastMovementAt,
            long daysOverdue) {
    }

    /** All PENDING tasks whose due date is before today. */
    List<OverdueTask> findOverdueTasks(LocalDate today);

    /** One task's immutable SLA facts for the monitoring timeline. */
    Optional<OverdueTask> findTaskSla(UUID taskId, LocalDate today);

    /** Organization-wide live SLA counters sourced from workflow_tasks. */
    record SlaCounters(long totalPending, long withinSla, long dueToday,
                       long dueTomorrow, long overdue, long criticalOverdue) {
    }

    SlaCounters organizationSlaCounters(LocalDate today, int criticalDays);

    /** Pending + overdue task counts for a dashboard scope. */
    record TaskMetrics(long pending, long overdue) {
    }

    TaskMetrics taskMetrics(UUID userId, UUID sectionId, LocalDate today);

    TaskMetrics taskMetricsBySections(java.util.Collection<UUID> sectionIds,
                                      LocalDate today);

    record LetterTaskState(UUID taskId, UUID workflowInstanceId, UUID stepId, String stepName,
                           Integer stepOrder, String workflowStatus, UUID assignedToUserId,
                           UUID assignedToSectionId, LocalDate dueDate,
                           LocalDateTime assignedAt) { }

    record LetterWorkflowMovement(UUID id, String action, UUID fromUserId, UUID toUserId,
                                  UUID fromSectionId, UUID toSectionId, String remarks,
                                  LocalDateTime actionAt) { }

    Optional<LetterTaskState> currentLetterTask(UUID letterId);

    List<LetterWorkflowMovement> letterMovementHistory(UUID letterId);

    TaskMetrics letterTaskMetrics(UUID userId, UUID sectionId, LocalDate today);

    TaskMetrics letterTaskMetricsBySections(
            java.util.Collection<UUID> sectionIds, LocalDate today);

    List<UUID> fileIdsMovedBy(UUID userId);

    List<UUID> fileIdsClosedBy(UUID userId);
}
