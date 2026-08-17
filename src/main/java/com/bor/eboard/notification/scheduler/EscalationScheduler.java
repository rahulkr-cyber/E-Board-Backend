package com.bor.eboard.notification.scheduler;

import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.notification.entity.EscalationRule;
import com.bor.eboard.notification.entity.TaskEscalation;
import com.bor.eboard.notification.facade.NotificationFacade;
import com.bor.eboard.notification.repository.EscalationRuleRepository;
import com.bor.eboard.notification.repository.TaskEscalationRepository;
import com.bor.eboard.workflow.facade.WorkflowEscalationFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily, data-driven escalation sweep (06_BUSINESS_RULES.md section 10).
 *
 * For each overdue pending task the scheduler records the first configured
 * breach rung and the highest currently crossed rung. Recording the first
 * rung separately guarantees Commissioner visibility even when a sweep was
 * missed while the application was offline. Each configured level can fire
 * only once because workflow_task_escalations has a unique
 * (task_id, escalation_level) constraint plus an existence check.
 *
 * The sweep is data-driven: thresholds and notify-roles live in
 * workflow_escalation_rules and can be reconfigured without code changes.
 */
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final WorkflowEscalationFacade workflowEscalationFacade;
    private final EscalationRuleRepository escalationRuleRepository;
    private final TaskEscalationRepository taskEscalationRepository;
    private final NotificationFacade notificationFacade;
    private final CorrespondenceFacade correspondenceFacade;
    private final IdentityFacade identityFacade;

    /** Runs every day at 01:00 server time. */
    @Scheduled(cron = "${escalation.cron:0 0 1 * * *}")
    @Transactional
    public void runDailyEscalationSweep() {
        LocalDate today = LocalDate.now();
        List<EscalationRule> rules =
                escalationRuleRepository.findByDeletedFalseAndActiveTrueOrderByDaysAfterDueAsc();
        if (rules.isEmpty()) {
            return;
        }
        List<WorkflowEscalationFacade.OverdueTask> overdue =
                workflowEscalationFacade.findOverdueTasks(today);
        int fired = 0;
        for (WorkflowEscalationFacade.OverdueTask task : overdue) {
            for (EscalationRule rule : rulesToFire(rules, task.daysOverdue())) {
                if (taskEscalationRepository.existsByTaskIdAndEscalationLevel(
                        task.taskId(), rule.getEscalationLevel())) {
                    continue; // already escalated at this configured level
                }
                recordAndNotify(task, rule);
                fired++;
            }
        }
        if (fired > 0) {
            log.info("Escalation sweep: {} task(s) escalated on {}", fired, today);
        }
    }

    private List<EscalationRule> rulesToFire(List<EscalationRule> rulesAsc, long daysOverdue) {
        EscalationRule first = null;
        EscalationRule highest = null;
        for (EscalationRule rule : rulesAsc) {
            if (daysOverdue >= rule.getDaysAfterDue()) {
                if (first == null) {
                    first = rule;
                }
                highest = rule; // ascending list -> last match is highest threshold
            }
        }
        if (first == null) {
            return List.of();
        }
        return first == highest ? List.of(first) : List.of(first, highest);
    }

    private void recordAndNotify(WorkflowEscalationFacade.OverdueTask task, EscalationRule rule) {
        TaskEscalation escalation = new TaskEscalation();
        escalation.setTaskId(task.taskId());
        escalation.setFileId(task.fileId());
        escalation.setLetterId(task.letterId());
        escalation.setEscalationLevel(rule.getEscalationLevel());
        escalation.setDaysAfterDue(rule.getDaysAfterDue());
        taskEscalationRepository.save(escalation);

        boolean letterTask = task.letterId() != null;
        String subject;
        String reference;
        if (letterTask) {
            CorrespondenceFacade.LetterState state = correspondenceFacade
                    .findLetterState(task.letterId()).orElse(null);
            subject = state != null && state.subject() != null ? state.subject() : "";
            reference = state != null && state.letterNumber() != null
                    ? state.letterNumber() : "Letter";
        } else {
            CorrespondenceFacade.FileState state = correspondenceFacade
                    .findFileState(task.fileId()).orElse(null);
            subject = state != null && state.subject() != null ? state.subject() : "";
            reference = state != null && state.fileNumber() != null
                    ? state.fileNumber() : "File";
        }
        Map<String, String> values = Map.of(
                "fileNumber", reference,
                "reference", reference,
                "subject", subject,
                "level", rule.getEscalationLevel(),
                "days", String.valueOf(task.daysOverdue()));
        String linkedEntityType = letterTask ? "LETTER" : "FILE";
        java.util.UUID linkedEntityId = letterTask ? task.letterId() : task.fileId();

        // Visibility escalation never mutates task ownership. Notify each
        // resolved recipient at most once for this configured escalation level.
        java.util.Set<UUID> recipients = new java.util.LinkedHashSet<>();
        if (task.assignedToUserId() != null) {
            recipients.add(task.assignedToUserId());
        }
        if (rule.getNotifyRoleId() != null) {
            identityFacade.resolveStepAssignee(null, rule.getNotifyRoleId(), null, null)
                    .ifPresent(recipients::add);
        }
        for (UUID recipient : recipients) {
            notificationFacade.notifyFromTemplate(recipient, "ESCALATION", values,
                    linkedEntityType, linkedEntityId, rule.getEscalationLevel());
        }
    }
}
