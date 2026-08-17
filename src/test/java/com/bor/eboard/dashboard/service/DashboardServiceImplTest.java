package com.bor.eboard.dashboard.service;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.facade.ChargeFacade;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.dashboard.dto.DashboardMetrics;
import com.bor.eboard.dashboard.dto.DashboardQuery;
import com.bor.eboard.dashboard.dto.DashboardScopeType;
import com.bor.eboard.dashboard.dto.SlaAgeing;
import com.bor.eboard.dashboard.service.impl.DashboardServiceImpl;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.notification.facade.NotificationFacade;
import com.bor.eboard.registry.facade.RegistryFacade;
import com.bor.eboard.workflow.facade.WorkflowEscalationFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceImplTest {

    @Mock private CorrespondenceFacade correspondenceFacade;
    @Mock private WorkflowEscalationFacade workflowEscalationFacade;
    @Mock private RegistryFacade registryFacade;
    @Mock private IdentityFacade identityFacade;
    @Mock private AuditService auditService;
    @Mock private ChargeFacade chargeFacade;
    @Mock private ChecklistService checklistService;
    @Mock private MasterDataFacade masterDataFacade;
    @Mock private NotificationFacade notificationFacade;

    @InjectMocks private DashboardServiceImpl service;

    private UUID currentUserId;

    @BeforeEach
    void authenticate() {
        currentUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUserId.toString(), "n/a",
                        List.of(
                                new SimpleGrantedAuthority("DASHBOARD_VIEW"),
                                new SimpleGrantedAuthority("DASHBOARD_VIEW_SELF"),
                                new SimpleGrantedAuthority("DASHBOARD_VIEW_SECTION"),
                                new SimpleGrantedAuthority("DASHBOARD_VIEW_DEPARTMENT"),
                                new SimpleGrantedAuthority("DASHBOARD_VIEW_OFFICER"),
                                new SimpleGrantedAuthority("DASHBOARD_VIEW_USER"),
                                new SimpleGrantedAuthority("DASHBOARD_VIEW_ORGANIZATION"))));
        stubEmptyFacades();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void stubEmptyFacades() {
        lenient().when(correspondenceFacade.fileMetrics(
                        nullable(UUID.class), nullable(UUID.class)))
                .thenReturn(new CorrespondenceFacade.FileMetrics(0, 0, 0, 0, 0, 0, 0, 0));
        lenient().when(correspondenceFacade.fileActivity(
                        nullable(UUID.class), nullable(UUID.class),
                        any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new CorrespondenceFacade.FileActivityMetrics(0, 0));
        lenient().when(workflowEscalationFacade.taskMetrics(
                        nullable(UUID.class), nullable(UUID.class), any(LocalDate.class)))
                .thenReturn(new WorkflowEscalationFacade.TaskMetrics(0, 0));
        lenient().when(workflowEscalationFacade.letterTaskMetrics(
                        nullable(UUID.class), nullable(UUID.class), any(LocalDate.class)))
                .thenReturn(new WorkflowEscalationFacade.TaskMetrics(0, 0));
        lenient().when(registryFacade.diariesReceivedToday(nullable(UUID.class))).thenReturn(0L);
        lenient().when(registryFacade.diariesReceivedBetween(
                        any(LocalDate.class), any(LocalDate.class), nullable(UUID.class)))
                .thenReturn(0L);
        lenient().when(correspondenceFacade.diaryLetterMetrics(
                        nullable(UUID.class), nullable(UUID.class)))
                .thenReturn(new CorrespondenceFacade.DiaryLetterMetrics(0, 0));
        lenient().when(correspondenceFacade.recentFiles(
                        nullable(UUID.class), nullable(UUID.class),
                        nullable(LocalDate.class), nullable(LocalDate.class), anyInt()))
                .thenReturn(List.of());
        lenient().when(workflowEscalationFacade.findOverdueTasks(any(LocalDate.class)))
                .thenReturn(List.of());
        lenient().when(chargeFacade.operationalMetrics(any(), any(LocalDate.class)))
                .thenReturn(new ChargeFacade.OperationalMetrics(0, 0, 0, 0, 0));
        lenient().when(notificationFacade.activeEscalationRules()).thenReturn(List.of());
    }

    private WorkflowEscalationFacade.OverdueTask overdue(long days) {
        return new WorkflowEscalationFacade.OverdueTask(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), "Review", UUID.randomUUID(), null, UUID.randomUUID(),
                LocalDate.now().minusDays(days), java.time.LocalDateTime.now().minusDays(days + 1),
                java.time.LocalDateTime.now().minusDays(days), days);
    }

    @Test
    @DisplayName("buckets overdue tasks cumulative-exclusively by SLA threshold")
    void slaAgeingBuckets() {
        when(workflowEscalationFacade.taskMetrics(
                nullable(UUID.class), nullable(UUID.class), any(LocalDate.class)))
                .thenReturn(new WorkflowEscalationFacade.TaskMetrics(10, 6));
        when(workflowEscalationFacade.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of(
                overdue(2),
                overdue(4),
                overdue(9),
                overdue(20),
                overdue(31),
                overdue(45)));

        DashboardMetrics metrics = service.global("Test Dashboard");

        SlaAgeing ageing = metrics.getSlaAgeing();
        assertThat(ageing.getReminder()).isEqualTo(1);
        assertThat(ageing.getWarning()).isEqualTo(1);
        assertThat(ageing.getEscalation()).isEqualTo(1);
        assertThat(ageing.getHighestEscalation()).isEqualTo(2);
        assertThat(ageing.getOnTrack()).isEqualTo(4);
    }

    @Test
    @DisplayName("composes headline and range-aware widgets from existing facades")
    void composesWidgets() {
        when(correspondenceFacade.fileMetrics(
                nullable(UUID.class), nullable(UUID.class)))
                .thenReturn(new CorrespondenceFacade.FileMetrics(3, 2, 4, 1, 0, 7, 0, 9));
        when(correspondenceFacade.fileActivity(
                nullable(UUID.class), nullable(UUID.class),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new CorrespondenceFacade.FileActivityMetrics(6, 4));
        when(workflowEscalationFacade.taskMetrics(
                nullable(UUID.class), nullable(UUID.class), any(LocalDate.class)))
                .thenReturn(new WorkflowEscalationFacade.TaskMetrics(12, 3));
        when(registryFacade.diariesReceivedToday(nullable(UUID.class))).thenReturn(5L);
        when(registryFacade.diariesReceivedBetween(
                any(LocalDate.class), any(LocalDate.class), nullable(UUID.class)))
                .thenReturn(8L);

        DashboardMetrics metrics = service.get(new DashboardQuery(
                DashboardScopeType.ORGANIZATION, null,
                LocalDate.now().minusDays(6), LocalDate.now(), "Weekly review", true));

        assertThat(metrics.getPendingFiles()).isEqualTo(12);
        assertThat(metrics.getOverdueFiles()).isEqualTo(3);
        assertThat(metrics.getReceivedToday()).isEqualTo(5);
        assertThat(metrics.getDisposedThisMonth()).isEqualTo(9);
        assertThat(metrics.getPendingApprovals()).isEqualTo(4);
        assertThat(metrics.getActivityReceived()).isEqualTo(8);
        assertThat(metrics.getActivityOpened()).isEqualTo(6);
        assertThat(metrics.getActivityDisposed()).isEqualTo(4);
        assertThat(metrics.getScopeLabel()).isEqualTo("Entire Organization");
    }

    @Test
    @DisplayName("self scope remains the default dashboard for every authenticated user")
    void selfIsDefault() {
        UUID sectionId = UUID.randomUUID();
        when(identityFacade.findUser(currentUserId)).thenReturn(java.util.Optional.of(
                new IdentityFacade.UserRef(
                        currentUserId, sectionId, UUID.randomUUID(), null,
                        "Current User", true)));

        DashboardMetrics metrics = service.get(null);

        assertThat(metrics.getScope()).isEqualTo("SELF");
        assertThat(metrics.getScopeId()).isEqualTo(currentUserId);
        assertThat(metrics.getScopeLabel()).isEqualTo("My Dashboard");
    }
    @Test
    @DisplayName("department scope uses batched section-set queries")
    void departmentScopeUsesBatchedQueries() {
        UUID departmentId = UUID.randomUUID();
        UUID sectionOne = UUID.randomUUID();
        UUID sectionTwo = UUID.randomUUID();
        List<UUID> sectionIds = List.of(sectionOne, sectionTwo);

        when(identityFacade.activeDepartmentNames())
                .thenReturn(java.util.Map.of(departmentId, "Revenue Administration"));
        when(identityFacade.activeSections(departmentId)).thenReturn(List.of(
                new IdentityFacade.SectionRef(sectionOne, departmentId, "Section 1"),
                new IdentityFacade.SectionRef(sectionTwo, departmentId, "Section 2")));
        when(correspondenceFacade.fileMetricsBySections(sectionIds))
                .thenReturn(new CorrespondenceFacade.FileMetrics(1, 2, 3, 4, 5, 6, 7, 8));
        when(correspondenceFacade.fileActivityBySections(
                org.mockito.ArgumentMatchers.eq(sectionIds),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new CorrespondenceFacade.FileActivityMetrics(9, 10));
        when(workflowEscalationFacade.taskMetricsBySections(
                org.mockito.ArgumentMatchers.eq(sectionIds), any(LocalDate.class)))
                .thenReturn(new WorkflowEscalationFacade.TaskMetrics(11, 12));
        when(workflowEscalationFacade.letterTaskMetricsBySections(
                org.mockito.ArgumentMatchers.eq(sectionIds), any(LocalDate.class)))
                .thenReturn(new WorkflowEscalationFacade.TaskMetrics(13, 14));
        when(registryFacade.diariesReceivedBetweenSections(
                any(LocalDate.class), any(LocalDate.class),
                org.mockito.ArgumentMatchers.eq(sectionIds)))
                .thenReturn(15L, 16L);
        when(correspondenceFacade.diaryLetterMetricsBySections(sectionIds))
                .thenReturn(new CorrespondenceFacade.DiaryLetterMetrics(17, 18));
        when(correspondenceFacade.recentFilesBySections(
                org.mockito.ArgumentMatchers.eq(sectionIds),
                nullable(LocalDate.class), nullable(LocalDate.class), anyInt()))
                .thenReturn(List.of());

        DashboardMetrics metrics = service.get(new DashboardQuery(
                DashboardScopeType.DEPARTMENT, departmentId,
                null, null, "Department review", true));

        assertThat(metrics.getScope()).isEqualTo("DEPARTMENT");
        assertThat(metrics.getScopeLabel()).isEqualTo("Revenue Administration");
        assertThat(metrics.getPendingFiles()).isEqualTo(11);
        assertThat(metrics.getActivityOpened()).isEqualTo(9);
        assertThat(metrics.getTrackingDiaries()).isEqualTo(17);
        verify(correspondenceFacade).fileMetricsBySections(sectionIds);
        verify(correspondenceFacade, never()).fileMetrics(null, sectionOne);
        verify(correspondenceFacade, never()).fileMetrics(null, sectionTwo);
    }

}
