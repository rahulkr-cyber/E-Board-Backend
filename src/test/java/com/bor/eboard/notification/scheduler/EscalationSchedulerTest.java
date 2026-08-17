package com.bor.eboard.notification.scheduler;

import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.notification.entity.EscalationRule;
import com.bor.eboard.notification.entity.TaskEscalation;
import com.bor.eboard.notification.facade.NotificationFacade;
import com.bor.eboard.notification.repository.EscalationRuleRepository;
import com.bor.eboard.notification.repository.TaskEscalationRepository;
import com.bor.eboard.workflow.facade.WorkflowEscalationFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscalationScheduler")
class EscalationSchedulerTest {

    @Mock private WorkflowEscalationFacade workflowEscalationFacade;
    @Mock private EscalationRuleRepository escalationRuleRepository;
    @Mock private TaskEscalationRepository taskEscalationRepository;
    @Mock private NotificationFacade notificationFacade;
    @Mock private CorrespondenceFacade correspondenceFacade;
    @Mock private IdentityFacade identityFacade;

    @InjectMocks private EscalationScheduler scheduler;

    private EscalationRule rule(String code, int days, String level, UUID notifyRole) {
        EscalationRule r = new EscalationRule();
        r.setCode(code);
        r.setName(code);
        r.setDaysAfterDue(days);
        r.setEscalationLevel(level);
        r.setNotifyRoleId(notifyRole);
        r.setActive(true);
        return r;
    }

    private WorkflowEscalationFacade.OverdueTask task(long days) {
        return new WorkflowEscalationFacade.OverdueTask(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), "Review", UUID.randomUUID(), null, UUID.randomUUID(),
                LocalDate.now().minusDays(days), java.time.LocalDateTime.now().minusDays(days + 1),
                java.time.LocalDateTime.now().minusDays(days), days);
    }

    private List<EscalationRule> ladder() {
        return List.of(
                rule("OVERDUE_1D", 1, "OVERDUE", UUID.randomUUID()),
                rule("REMINDER_3D", 3, "REMINDER", null),
                rule("WARNING_7D", 7, "WARNING", null),
                rule("ESCALATION_15D", 15, "ESCALATION", UUID.randomUUID()),
                rule("TOP_30D", 30, "TOP_ESCALATION", UUID.randomUUID()));
    }

    @Test
    @DisplayName("records initial breach visibility and the highest crossed rung")
    void firesHighestRung() {
        when(escalationRuleRepository.findByDeletedFalseAndActiveTrueOrderByDaysAfterDueAsc())
                .thenReturn(ladder());
        when(workflowEscalationFacade.findOverdueTasks(any(LocalDate.class)))
                .thenReturn(List.of(task(20))); // crosses 3,7,15 -> highest is ESCALATION(15)
        when(taskEscalationRepository.existsByTaskIdAndEscalationLevel(any(), anyString()))
                .thenReturn(false);
        lenient().when(correspondenceFacade.findFileState(any())).thenReturn(Optional.empty());
        lenient().when(identityFacade.resolveStepAssignee(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        scheduler.runDailyEscalationSweep();

        ArgumentCaptor<TaskEscalation> captor = ArgumentCaptor.forClass(TaskEscalation.class);
        verify(taskEscalationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(TaskEscalation::getEscalationLevel)
                .containsExactly("OVERDUE", "ESCALATION");
    }

    @Test
    @DisplayName("does not re-fire a level already recorded for the task")
    void dedupsAlreadyFiredLevel() {
        when(escalationRuleRepository.findByDeletedFalseAndActiveTrueOrderByDaysAfterDueAsc())
                .thenReturn(ladder());
        when(workflowEscalationFacade.findOverdueTasks(any(LocalDate.class)))
                .thenReturn(List.of(task(9))); // highest crossed is WARNING(7)
        when(taskEscalationRepository.existsByTaskIdAndEscalationLevel(any(), anyString()))
                .thenReturn(true);

        scheduler.runDailyEscalationSweep();

        verify(taskEscalationRepository, never()).save(any());
        verify(notificationFacade, never())
                .notifyFromTemplate(any(), anyString(), anyMap(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("notifies both the task holder and the configured authority")
    void notifiesHolderAndAuthority() {
        UUID authorityRole = UUID.randomUUID();
        UUID authorityUser = UUID.randomUUID();
        List<EscalationRule> rules = List.of(rule("ESCALATION_15D", 15, "ESCALATION", authorityRole));

        when(escalationRuleRepository.findByDeletedFalseAndActiveTrueOrderByDaysAfterDueAsc())
                .thenReturn(rules);
        WorkflowEscalationFacade.OverdueTask overdue = task(16);
        when(workflowEscalationFacade.findOverdueTasks(any(LocalDate.class)))
                .thenReturn(List.of(overdue));
        when(taskEscalationRepository.existsByTaskIdAndEscalationLevel(any(), anyString()))
                .thenReturn(false);
        when(correspondenceFacade.findFileState(any())).thenReturn(Optional.empty());
        when(identityFacade.resolveStepAssignee(
                org.mockito.ArgumentMatchers.isNull(), eq(authorityRole),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(Optional.of(authorityUser));

        scheduler.runDailyEscalationSweep();

        // holder + authority = 2 notifications
        verify(notificationFacade, times(2))
                .notifyFromTemplate(any(), eq("ESCALATION"), anyMap(), eq("FILE"), any(), anyString());
    }
}
