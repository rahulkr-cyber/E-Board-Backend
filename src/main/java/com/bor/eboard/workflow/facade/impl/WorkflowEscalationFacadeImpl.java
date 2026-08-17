package com.bor.eboard.workflow.facade.impl;

import com.bor.eboard.workflow.entity.WorkflowStep;
import com.bor.eboard.workflow.entity.WorkflowTask;
import com.bor.eboard.workflow.facade.WorkflowEscalationFacade;
import com.bor.eboard.workflow.repository.WorkflowInstanceRepository;
import com.bor.eboard.workflow.repository.WorkflowMovementRepository;
import com.bor.eboard.workflow.repository.WorkflowStepRepository;
import com.bor.eboard.workflow.repository.WorkflowTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkflowEscalationFacadeImpl implements WorkflowEscalationFacade {

    private final WorkflowTaskRepository taskRepository;
    private final WorkflowMovementRepository movementRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OverdueTask> findOverdueTasks(LocalDate today) {
        List<WorkflowTask> tasks = taskRepository.findOverdue(today);
        return enrich(tasks, today);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OverdueTask> findTaskSla(UUID taskId, LocalDate today) {
        return taskRepository.findById(taskId)
                .filter(task -> "PENDING".equals(task.getStatus())
                        && task.getDueDate() != null && task.getDueDate().isBefore(today))
                .map(List::of)
                .map(tasks -> enrich(tasks, today))
                .filter(tasks -> !tasks.isEmpty())
                .map(tasks -> tasks.get(0));
    }

    private List<OverdueTask> enrich(List<WorkflowTask> tasks, LocalDate today) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Map<UUID, WorkflowStep> steps = stepRepository.findAllById(tasks.stream()
                        .map(WorkflowTask::getStepId).distinct().toList()).stream()
                .collect(Collectors.toMap(WorkflowStep::getId, Function.identity()));
        Collection<UUID> instanceIds = tasks.stream()
                .map(WorkflowTask::getWorkflowInstanceId).distinct().toList();
        Map<UUID, LocalDateTime> lastMovements = instanceIds.isEmpty() ? Map.of()
                : movementRepository.findLastMovementByWorkflowInstanceIds(instanceIds).stream()
                .collect(Collectors.toMap(
                        WorkflowMovementRepository.LastMovementProjection::getWorkflowInstanceId,
                        WorkflowMovementRepository.LastMovementProjection::getLastMovementAt));
        return tasks.stream().map(task -> {
            WorkflowStep step = steps.get(task.getStepId());
            long daysOverdue = task.getDueDate() == null ? 0
                    : Math.max(ChronoUnit.DAYS.between(task.getDueDate(), today), 0);
            return new OverdueTask(task.getId(), task.getWorkflowInstanceId(),
                    task.getFileId(), task.getLetterId(), task.getStepId(),
                    step != null ? step.getStepName() : null,
                    task.getAssignedToUserId(), task.getAssignedToRoleId(),
                    task.getAssignedToSectionId(), task.getDueDate(), task.getAssignedAt(),
                    lastMovements.getOrDefault(task.getWorkflowInstanceId(), task.getAssignedAt()),
                    daysOverdue);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SlaCounters organizationSlaCounters(LocalDate today, int criticalDays) {
        long pending = taskRepository.countPendingByScope(null, null);
        long overdue = taskRepository.countOverdueByScope(null, null, today);
        long dueToday = taskRepository.countPendingDueOn(today);
        long dueTomorrow = taskRepository.countPendingDueOn(today.plusDays(1));
        int safeCriticalDays = Math.max(criticalDays, 1);
        long critical = taskRepository.countCriticalOverdue(today.minusDays(safeCriticalDays));
        return new SlaCounters(pending, Math.max(pending - overdue, 0), dueToday,
                dueTomorrow, overdue, critical);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskMetrics taskMetrics(UUID userId, UUID sectionId, LocalDate today) {
        return new TaskMetrics(taskRepository.countPendingByScope(userId, sectionId),
                taskRepository.countOverdueByScope(userId, sectionId, today));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskMetrics taskMetricsBySections(Collection<UUID> sectionIds, LocalDate today) {
        if (sectionIds == null || sectionIds.isEmpty()) return new TaskMetrics(0, 0);
        return new TaskMetrics(taskRepository.countPendingBySections(sectionIds),
                taskRepository.countOverdueBySections(sectionIds, today));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LetterTaskState> currentLetterTask(UUID letterId) {
        var pending = taskRepository.findCurrentTask(letterId);
        if (pending.isPresent()) {
            var task = pending.get();
            var instance = instanceRepository.findById(task.getWorkflowInstanceId()).orElse(null);
            var step = stepRepository.findById(task.getStepId()).orElse(null);
            return Optional.of(new LetterTaskState(task.getId(), task.getWorkflowInstanceId(),
                    task.getStepId(), step != null ? step.getStepName() : null,
                    step != null ? step.getStepOrder() : null,
                    instance != null ? instance.getStatus() : null, task.getAssignedToUserId(),
                    task.getAssignedToSectionId(), task.getDueDate(), task.getAssignedAt()));
        }
        return instanceRepository.findByLetterIdOrderByStartedAtDesc(letterId).stream().findFirst()
                .map(instance -> {
                    var step = instance.getCurrentStepId() != null
                            ? stepRepository.findById(instance.getCurrentStepId()).orElse(null) : null;
                    return new LetterTaskState(null, instance.getId(), instance.getCurrentStepId(),
                            step != null ? step.getStepName() : null, instance.getCurrentStepOrder(),
                            instance.getStatus(), null, null, null, instance.getStartedAt());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<LetterWorkflowMovement> letterMovementHistory(UUID letterId) {
        return movementRepository.findByLetterIdOrderByActionAtAsc(letterId).stream()
                .map(m -> new LetterWorkflowMovement(m.getId(), m.getAction(), m.getFromUserId(),
                        m.getToUserId(), m.getFromSectionId(), m.getToSectionId(),
                        m.getRemarks(), m.getActionAt())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskMetrics letterTaskMetrics(UUID userId, UUID sectionId, LocalDate today) {
        return new TaskMetrics(taskRepository.countPendingLettersByScope(userId, sectionId),
                taskRepository.countOverdueLettersByScope(userId, sectionId, today));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskMetrics letterTaskMetricsBySections(Collection<UUID> sectionIds, LocalDate today) {
        if (sectionIds == null || sectionIds.isEmpty()) return new TaskMetrics(0, 0);
        return new TaskMetrics(taskRepository.countPendingLettersBySections(sectionIds),
                taskRepository.countOverdueLettersBySections(sectionIds, today));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> fileIdsMovedBy(UUID userId) {
        return userId == null ? List.of() : movementRepository.findFileIdsMovedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> fileIdsClosedBy(UUID userId) {
        return userId == null ? List.of() : movementRepository.findFileIdsClosedBy(userId);
    }
}
