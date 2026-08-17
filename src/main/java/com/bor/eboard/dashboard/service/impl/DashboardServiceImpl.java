package com.bor.eboard.dashboard.service.impl;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.facade.ChargeFacade;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.dashboard.dto.DashboardMetrics;
import com.bor.eboard.dashboard.dto.DashboardQuery;
import com.bor.eboard.dashboard.dto.DashboardScopeOption;
import com.bor.eboard.dashboard.dto.DashboardScopeOptionsResponse;
import com.bor.eboard.dashboard.dto.DashboardScopeType;
import com.bor.eboard.dashboard.dto.OverdueDashboardQuery;
import com.bor.eboard.dashboard.dto.OverdueDashboardResponse;
import com.bor.eboard.dashboard.dto.RecentFileRow;
import com.bor.eboard.dashboard.dto.SlaTimelineEvent;
import com.bor.eboard.dashboard.dto.SlaAgeing;
import com.bor.eboard.dashboard.facade.DashboardScopeFacade;
import com.bor.eboard.dashboard.service.DashboardService;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.notification.facade.NotificationFacade;
import com.bor.eboard.registry.facade.RegistryFacade;
import com.bor.eboard.workflow.facade.WorkflowEscalationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService, DashboardScopeFacade {

    private static final int RECENT_LIMIT = 10;
    private static final int SCOPE_OPTION_LIMIT = 50;
    private static final int MAX_ACTIVITY_RANGE_DAYS = 1_826; // five years
    private static final String LEGACY_VIEW_PERMISSION = "DASHBOARD_VIEW";
    private static final String MODULE = "DASHBOARD";

    private static final Map<DashboardScopeType, String> SCOPE_PERMISSIONS =
            new EnumMap<>(DashboardScopeType.class);

    static {
        SCOPE_PERMISSIONS.put(DashboardScopeType.SELF, "DASHBOARD_VIEW_SELF");
        SCOPE_PERMISSIONS.put(DashboardScopeType.SECTION, "DASHBOARD_VIEW_SECTION");
        SCOPE_PERMISSIONS.put(DashboardScopeType.DEPARTMENT, "DASHBOARD_VIEW_DEPARTMENT");
        SCOPE_PERMISSIONS.put(DashboardScopeType.OFFICER, "DASHBOARD_VIEW_OFFICER");
        SCOPE_PERMISSIONS.put(DashboardScopeType.USER, "DASHBOARD_VIEW_USER");
        SCOPE_PERMISSIONS.put(DashboardScopeType.ORGANIZATION, "DASHBOARD_VIEW_ORGANIZATION");
    }

    private final CorrespondenceFacade correspondenceFacade;
    private final WorkflowEscalationFacade workflowEscalationFacade;
    private final RegistryFacade registryFacade;
    private final IdentityFacade identityFacade;
    private final ChargeFacade chargeFacade;
    private final AuditService auditService;
    private final ChecklistService checklistService;
    private final MasterDataFacade masterDataFacade;
    private final NotificationFacade notificationFacade;

    @Override
    @Transactional(readOnly = true)
    public DashboardMetrics get(DashboardQuery query) {
        UUID currentUserId = currentUser();
        DashboardQuery safeQuery = query == null
                ? new DashboardQuery(DashboardScopeType.SELF, null, null, null, null, false)
                : query;
        DashboardScopeType scopeType = safeQuery.effectiveScopeType();
        requireScopePermission(scopeType);
        validateDates(safeQuery.fromDate(), safeQuery.toDate());
        validateReason(safeQuery.reason());

        ScopeResolution scope = resolveScope(scopeType, safeQuery.scopeId(), currentUserId);
        LocalDate today = LocalDate.now();
        LocalDate activityFrom = safeQuery.fromDate() != null ? safeQuery.fromDate() : today;
        LocalDate activityTo = safeQuery.toDate() != null ? safeQuery.toDate() : today;

        ScopeTotals totals = aggregate(scope, activityFrom, activityTo, today);
        SlaAgeing ageing = buildAgeing(scope, today, totals.tasks().pending());
        List<RecentFileRow> recent = recentFiles(
                scope, safeQuery.fromDate(), safeQuery.toDate());
        List<IdentityFacade.EmployeeRef> scopedEmployees = employeesInScope(scope);
        Set<UUID> scopedEmployeeIds = scopedEmployees.stream()
                .map(IdentityFacade.EmployeeRef::id)
                .collect(Collectors.toSet());
        ChargeFacade.OperationalMetrics postingMetrics =
                chargeFacade.operationalMetrics(new ChargeFacade.OperationalScope(
                        scopedEmployeeIds, Set.copyOf(scope.sectionIds()),
                        scope.ownerId(), scope.organization()), today);
        long currentPosting = scopedEmployees.stream()
                .filter(employee -> "ACTIVE".equals(employee.status()))
                .filter(employee -> employee.departmentId() != null
                        && employee.sectionId() != null
                        && employee.designationId() != null)
                .filter(employee -> employee.dateOfBirth() == null
                        || employee.dateOfBirth().plusYears(60).isAfter(today))
                .count();
        long upcomingRetirements = scopedEmployees.stream()
                .filter(employee -> employee.dateOfBirth() != null)
                .map(employee -> employee.dateOfBirth().plusYears(60))
                .filter(date -> !date.isBefore(today)
                        && !date.isAfter(today.plusMonths(6)))
                .count();
        long retiredEmployees = scopedEmployees.stream()
                .filter(employee -> employee.dateOfBirth() != null)
                .map(employee -> employee.dateOfBirth().plusYears(60))
                .filter(date -> date.isBefore(today))
                .count();

        DashboardMetrics metrics = DashboardMetrics.builder()
                .scope(scope.type().name())
                .scopeId(scope.id())
                .scopeLabel(scope.label())
                .activityFrom(activityFrom)
                .activityTo(activityTo)
                .activityReceived(totals.activityReceived())
                .activityOpened(totals.fileActivity().opened())
                .activityDisposed(totals.fileActivity().disposed())
                .pendingFiles(totals.tasks().pending())
                .overdueFiles(totals.tasks().overdue())
                .receivedToday(totals.receivedToday())
                .disposedThisMonth(totals.files().disposedThisMonth())
                .returnedFiles(totals.files().returned())
                .pendingApprovals(totals.files().pendingApproval())
                .rejectedFiles(totals.files().rejected())
                .trackingDiaries(totals.diaryLetters().forwarded())
                .overdueDiaries(totals.diaryTasks().overdue())
                .disposedDiaries(totals.diaryLetters().disposed())
                .currentPosting(currentPosting)
                .additionalCharges(postingMetrics.additionalCharges())
                .vacantSeats(postingMetrics.vacantSeats())
                .joiningPending(postingMetrics.joiningPending())
                .relievingPending(postingMetrics.relievingPending())
                .transfersToday(postingMetrics.transfersToday())
                .upcomingRetirements(upcomingRetirements)
                .retiredEmployees(retiredEmployees)
                .slaAgeing(ageing)
                .checklist(checklistService.dashboard(scope.ownerId(), scope.sectionIds(), scope.organization()))
                .recentFiles(recent)
                .build();

        if (safeQuery.viewAsAction()) {
            auditViewAs(scope, safeQuery.reason());
        }
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeScope resolveEmployeeScope(DashboardScopeType scopeType, UUID scopeId) {
        UUID currentUserId = currentUser();
        DashboardScopeType effectiveScope = scopeType == null
                ? DashboardScopeType.SELF : scopeType;
        requireScopePermission(effectiveScope);
        ScopeResolution scope = resolveScope(effectiveScope, scopeId, currentUserId);
        return new EmployeeScope(scope.type(), scope.id(), scope.label(),
                employeesInScope(scope));
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardScopeOptionsResponse scopeOptions(DashboardScopeType scopeType,
                                                       UUID parentId,
                                                       String query) {
        UUID userId = currentUser();
        List<DashboardScopeType> allowed = allowedScopes();
        if (scopeType == null) {
            return new DashboardScopeOptionsResponse(allowed, List.of());
        }
        if (!allowed.contains(scopeType)) {
            throw new ForbiddenException("Dashboard scope is not permitted: " + scopeType);
        }

        Map<UUID, String> departments = identityFacade.activeDepartmentNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        List<DashboardScopeOption> options = switch (scopeType) {
            case SELF -> identityFacade.findUser(userId)
                    .map(user -> List.of(new DashboardScopeOption(
                            user.id(), "My Dashboard", user.fullName())))
                    .orElse(List.of());
            case ORGANIZATION -> List.of(new DashboardScopeOption(
                    null, "Entire Organization", "All departments and sections"));
            case DEPARTMENT -> departments.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                    .map(entry -> new DashboardScopeOption(
                            entry.getKey(), entry.getValue(), "Department"))
                    .toList();
            case SECTION -> identityFacade.activeSections(parentId).stream()
                    .map(section -> new DashboardScopeOption(
                            section.id(), section.name(), departments.get(section.departmentId())))
                    .toList();
            case USER, OFFICER -> identityFacade.searchActiveUsers(
                            query, parentId, null, SCOPE_OPTION_LIMIT).stream()
                    .map(user -> new DashboardScopeOption(
                            user.id(), user.fullName(), userContext(user, departments, sections)))
                    .toList();
        };
        return new DashboardScopeOptionsResponse(allowed, options);
    }

    @Override
    public DashboardMetrics personal() {
        return get(new DashboardQuery(DashboardScopeType.SELF, null, null, null, null, false));
    }

    @Override
    public DashboardMetrics section() {
        return get(new DashboardQuery(DashboardScopeType.SECTION, null, null, null, null, false));
    }

    @Override
    public DashboardMetrics global(String scopeLabel) {
        DashboardMetrics metrics = get(new DashboardQuery(
                DashboardScopeType.ORGANIZATION, null, null, null, null, false));
        if (scopeLabel != null && !scopeLabel.isBlank()) {
            metrics.setScopeLabel(scopeLabel.trim());
        }
        return metrics;
    }


    @Override
    @Transactional(readOnly = true)
    public OverdueDashboardResponse overdue(OverdueDashboardQuery query) {
        OverdueDashboardQuery safe = query == null
                ? new OverdueDashboardQuery(null, null, null, null, null, null, null,
                null, null, null, null, 0, 20, "daysOverdue", "desc") : query;
        validateOverdueQuery(safe);
        LocalDate today = LocalDate.now();
        OverdueData data = buildOverdueData(today);
        List<OverdueDashboardResponse.OverdueCaseRow> filtered = filterOverdue(data.rows(), safe);
        filtered = sortOverdue(filtered, safe.safeSortBy(), safe.ascending());

        int from = Math.min(safe.safePage() * safe.safeSize(), filtered.size());
        int to = Math.min(from + safe.safeSize(), filtered.size());
        List<OverdueDashboardResponse.OverdueCaseRow> content = filtered.subList(from, to);
        int totalPages = filtered.isEmpty() ? 0
                : (int) Math.ceil((double) filtered.size() / safe.safeSize());
        PageResponse<OverdueDashboardResponse.OverdueCaseRow> page =
                PageResponse.<OverdueDashboardResponse.OverdueCaseRow>builder()
                        .content(content).page(safe.safePage()).size(safe.safeSize())
                        .totalElements(filtered.size()).totalPages(totalPages)
                        .last(safe.safePage() >= Math.max(totalPages - 1, 0)).build();

        WorkflowEscalationFacade.SlaCounters live = workflowEscalationFacade
                .organizationSlaCounters(today, data.thresholds().criticalDays());
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        long overdueToday = data.rows().stream().filter(row -> row.daysOverdue() == 1).count();
        long overdueThisWeek = data.rows().stream()
                .filter(row -> row.dueDate() != null)
                .map(row -> row.dueDate().plusDays(1))
                .filter(breachedOn -> !breachedOn.isBefore(weekStart) && !breachedOn.isAfter(today))
                .count();
        var counters = new OverdueDashboardResponse.Counters(
                live.totalPending(), live.withinSla(), live.dueToday(), live.dueTomorrow(),
                live.overdue(), live.criticalOverdue(), overdueToday, overdueThisWeek);

        return new OverdueDashboardResponse(counters,
                breakdown(filtered, OverdueDashboardResponse.OverdueCaseRow::currentSection),
                breakdown(filtered, OverdueDashboardResponse.OverdueCaseRow::department),
                breakdown(filtered, OverdueDashboardResponse.OverdueCaseRow::currentOwner),
                breakdown(filtered, OverdueDashboardResponse.OverdueCaseRow::category),
                filterOptions(data.rows()), data.thresholds(), page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverdueDashboardResponse.OverdueCaseRow> overdueCasesForReport(
            OverdueDashboardQuery query) {
        OverdueDashboardQuery safe = query == null
                ? new OverdueDashboardQuery(null, null, null, null, null, null, null,
                null, null, null, null, 0, 100, "daysOverdue", "desc") : query;
        validateOverdueQuery(safe);
        return sortOverdue(filterOverdue(buildOverdueData(LocalDate.now()).rows(), safe),
                safe.safeSortBy(), safe.ascending());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaTimelineEvent> overdueTimeline(UUID taskId) {
        if (taskId == null) {
            throw new ValidationException("Workflow task is required");
        }
        WorkflowEscalationFacade.OverdueTask task = workflowEscalationFacade
                .findTaskSla(taskId, LocalDate.now())
                .orElseThrow(() -> new ValidationException("Workflow task not found"));
        List<SlaTimelineEvent> events = new ArrayList<>();
        if (task.assignedAt() != null) {
            events.add(new SlaTimelineEvent("SLA_STARTED", "SLA Started",
                    "Task assigned at workflow stage " + safeLabel(task.stepName()),
                    task.assignedAt()));
        }
        if (task.dueDate() != null) {
            events.add(new SlaTimelineEvent("SLA_DUE_DATE", "SLA Due Date",
                    "Due date calculated by the workflow SLA configuration",
                    task.dueDate().atStartOfDay()));
            if (LocalDate.now().isAfter(task.dueDate())) {
                events.add(new SlaTimelineEvent("SLA_BREACHED", "SLA Breached",
                        task.daysOverdue() + " day(s) overdue",
                        task.dueDate().plusDays(1).atStartOfDay()));
            }
        }
        for (NotificationFacade.EscalationState escalation
                : notificationFacade.escalationHistory(taskId)) {
            events.add(new SlaTimelineEvent("ESCALATION_LEVEL", "Escalation Level",
                    escalation.escalationLevel() + " at " + escalation.daysAfterDue()
                            + " day(s) overdue", escalation.escalatedAt()));
            events.add(new SlaTimelineEvent("NOTIFICATION_GENERATED", "Notification Generated",
                    "Escalation notification generated without changing workflow ownership",
                    escalation.escalatedAt()));
        }
        return events.stream().sorted(Comparator.comparing(SlaTimelineEvent::eventAt,
                Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    private OverdueData buildOverdueData(LocalDate today) {
        List<NotificationFacade.EscalationRuleView> rules = notificationFacade.activeEscalationRules();
        OverdueDashboardResponse.Thresholds thresholds = resolveThresholds(rules);
        List<WorkflowEscalationFacade.OverdueTask> tasks =
                workflowEscalationFacade.findOverdueTasks(today);
        if (tasks.isEmpty()) {
            return new OverdueData(List.of(), thresholds);
        }

        Set<UUID> fileIds = tasks.stream().map(WorkflowEscalationFacade.OverdueTask::fileId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> letterIds = tasks.stream().map(WorkflowEscalationFacade.OverdueTask::letterId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, CorrespondenceFacade.FileState> files = correspondenceFacade.findFileStates(fileIds);
        Map<UUID, CorrespondenceFacade.LetterState> letters = correspondenceFacade.findLetterStates(letterIds);
        Map<UUID, CorrespondenceFacade.FileLetterReference> fileLetters =
                correspondenceFacade.primaryLetterReferences(fileIds);

        Set<UUID> diaryIds = new java.util.HashSet<>();
        letters.values().stream().map(CorrespondenceFacade.LetterState::diaryEntryId)
                .filter(Objects::nonNull).forEach(diaryIds::add);
        fileLetters.values().stream().map(CorrespondenceFacade.FileLetterReference::diaryEntryId)
                .filter(Objects::nonNull).forEach(diaryIds::add);
        Map<UUID, String> diaryNumbers = registryFacade.diaryNumbers(diaryIds);

        Map<UUID, String> users = identityFacade.userNames();
        Map<UUID, String> roles = identityFacade.roleNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, UUID> sectionDepartments = identityFacade.activeSections(null).stream()
                .collect(Collectors.toMap(IdentityFacade.SectionRef::id,
                        IdentityFacade.SectionRef::departmentId, (left, right) -> left));
        Map<UUID, String> categories = masterDataFacade.categoryNames();
        Map<UUID, String> sourcePriorities = masterDataFacade.priorityNames();
        Map<UUID, NotificationFacade.EscalationState> escalations = notificationFacade
                .latestEscalations(tasks.stream().map(WorkflowEscalationFacade.OverdueTask::taskId)
                        .toList());

        List<OverdueDashboardResponse.OverdueCaseRow> rows = new ArrayList<>();
        for (WorkflowEscalationFacade.OverdueTask task : tasks) {
            CorrespondenceFacade.FileState file = task.fileId() == null ? null : files.get(task.fileId());
            CorrespondenceFacade.LetterState letter = task.letterId() == null ? null : letters.get(task.letterId());
            CorrespondenceFacade.FileLetterReference fileLetter = task.fileId() == null
                    ? null : fileLetters.get(task.fileId());

            UUID diaryId = letter != null ? letter.diaryEntryId()
                    : fileLetter != null ? fileLetter.diaryEntryId() : null;
            String letterNumber = letter != null ? letter.letterNumber()
                    : fileLetter != null ? fileLetter.letterNumber() : null;
            String subject = letter != null ? letter.subject() : file != null ? file.subject() : null;
            UUID categoryId = letter != null ? letter.categoryId() : file != null ? file.categoryId() : null;
            UUID sourcePriorityId = letter != null ? letter.priorityId() : file != null ? file.priorityId() : null;
            UUID sectionId = task.assignedToSectionId() != null ? task.assignedToSectionId()
                    : letter != null ? letter.currentSectionId()
                    : file != null ? file.currentSectionId() : null;
            UUID departmentId = sectionDepartments.get(sectionId);
            if (departmentId == null) {
                departmentId = letter != null && letter.departmentId() != null ? letter.departmentId()
                        : file != null ? file.departmentId() : null;
            }
            UUID ownerId = task.assignedToUserId() != null ? task.assignedToUserId()
                    : letter != null ? letter.currentOwnerId()
                    : file != null ? file.currentOwnerId() : null;
            String owner = ownerId != null ? users.get(ownerId)
                    : task.assignedToRoleId() != null ? roles.get(task.assignedToRoleId())
                    : sectionId != null ? safeLabel(sections.get(sectionId)) + " Queue" : "Unassigned";
            NotificationFacade.EscalationState escalation = escalations.get(task.taskId());

            rows.add(new OverdueDashboardResponse.OverdueCaseRow(
                    task.taskId(), task.workflowInstanceId(), task.fileId(), task.letterId(), diaryId,
                    diaryNumbers.get(diaryId), letterNumber, file != null ? file.fileNumber() : null,
                    safeLabel(subject), categoryId, safeLabel(categories.get(categoryId)),
                    departmentId, safeLabel(departments.get(departmentId)), sectionId,
                    safeLabel(sections.get(sectionId)), ownerId, safeLabel(owner), task.stepId(),
                    safeLabel(task.stepName()), severity(task.daysOverdue(), thresholds),
                    safeLabel(sourcePriorities.get(sourcePriorityId)), "BREACHED", task.daysOverdue(),
                    task.assignedAt(), task.dueDate(), task.assignedAt(),
                    escalation != null ? escalation.escalationLevel() : "OVERDUE",
                    task.lastMovementAt()));
        }
        return new OverdueData(rows, thresholds);
    }

    private OverdueDashboardResponse.Thresholds resolveThresholds(
            List<NotificationFacade.EscalationRuleView> rules) {
        List<NotificationFacade.EscalationRuleView> sorted = rules == null ? List.of()
                : rules.stream().sorted(Comparator.comparingInt(
                        NotificationFacade.EscalationRuleView::daysAfterDue)).toList();
        int high = sorted.stream()
                .filter(rule -> rule.escalationLevel() != null
                        && rule.escalationLevel().toUpperCase().contains("WARNING"))
                .mapToInt(NotificationFacade.EscalationRuleView::daysAfterDue).findFirst()
                .orElseGet(() -> sorted.stream().filter(rule -> rule.daysAfterDue() > 1)
                        .mapToInt(NotificationFacade.EscalationRuleView::daysAfterDue)
                        .findFirst().orElse(1));
        int critical = sorted.stream()
                .filter(rule -> rule.escalationLevel() != null
                        && rule.escalationLevel().toUpperCase().contains("ESCALATION")
                        && !rule.escalationLevel().toUpperCase().startsWith("TOP"))
                .mapToInt(NotificationFacade.EscalationRuleView::daysAfterDue).findFirst()
                .orElseGet(() -> sorted.stream()
                        .mapToInt(NotificationFacade.EscalationRuleView::daysAfterDue)
                        .max().orElse(high));
        return new OverdueDashboardResponse.Thresholds(Math.max(high, 1),
                Math.max(critical, Math.max(high, 1)));
    }

    private String severity(long days, OverdueDashboardResponse.Thresholds thresholds) {
        if (days >= thresholds.criticalDays()) return "CRITICAL";
        if (days >= thresholds.highDays()) return "HIGH";
        return "MEDIUM";
    }

    private List<OverdueDashboardResponse.OverdueCaseRow> filterOverdue(
            List<OverdueDashboardResponse.OverdueCaseRow> rows, OverdueDashboardQuery q) {
        return rows.stream()
                .filter(row -> q.departmentId() == null || q.departmentId().equals(row.departmentId()))
                .filter(row -> q.sectionId() == null || q.sectionId().equals(row.sectionId()))
                .filter(row -> q.officerId() == null || q.officerId().equals(row.currentOwnerId()))
                .filter(row -> q.categoryId() == null || q.categoryId().equals(row.categoryId()))
                .filter(row -> q.workflowStageId() == null || q.workflowStageId().equals(row.workflowStageId()))
                .filter(row -> blank(q.priority()) || q.priority().equalsIgnoreCase(row.priority()))
                .filter(row -> blank(q.escalationLevel())
                        || q.escalationLevel().equalsIgnoreCase(row.escalationLevel()))
                .filter(row -> q.fromDate() == null || (row.dueDate() != null
                        && !row.dueDate().isBefore(q.fromDate())))
                .filter(row -> q.toDate() == null || (row.dueDate() != null
                        && !row.dueDate().isAfter(q.toDate())))
                .filter(row -> q.minDaysOverdue() == null || row.daysOverdue() >= q.minDaysOverdue())
                .filter(row -> q.maxDaysOverdue() == null || row.daysOverdue() <= q.maxDaysOverdue())
                .toList();
    }

    private List<OverdueDashboardResponse.OverdueCaseRow> sortOverdue(
            List<OverdueDashboardResponse.OverdueCaseRow> rows, String sortBy, boolean asc) {
        Comparator<OverdueDashboardResponse.OverdueCaseRow> comparator = switch (sortBy) {
            case "dueDate" -> Comparator.comparing(OverdueDashboardResponse.OverdueCaseRow::dueDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "assignedDate" -> Comparator.comparing(
                    OverdueDashboardResponse.OverdueCaseRow::assignedDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "currentSection" -> Comparator.comparing(
                    OverdueDashboardResponse.OverdueCaseRow::currentSection,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "currentOwner" -> Comparator.comparing(
                    OverdueDashboardResponse.OverdueCaseRow::currentOwner,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "category" -> Comparator.comparing(
                    OverdueDashboardResponse.OverdueCaseRow::category,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparingLong(
                    OverdueDashboardResponse.OverdueCaseRow::daysOverdue);
        };
        if (!asc) comparator = comparator.reversed();
        return rows.stream().sorted(comparator.thenComparing(
                OverdueDashboardResponse.OverdueCaseRow::taskId)).toList();
    }

    private List<OverdueDashboardResponse.Breakdown> breakdown(
            List<OverdueDashboardResponse.OverdueCaseRow> rows,
            java.util.function.Function<OverdueDashboardResponse.OverdueCaseRow, String> classifier) {
        Map<String, Long> counts = rows.stream().collect(Collectors.groupingBy(
                row -> safeLabel(classifier.apply(row)), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> new OverdueDashboardResponse.Breakdown(entry.getKey(), entry.getValue()))
                .toList();
    }

    private OverdueDashboardResponse.FilterOptions filterOptions(
            List<OverdueDashboardResponse.OverdueCaseRow> rows) {
        return new OverdueDashboardResponse.FilterOptions(
                options(rows, OverdueDashboardResponse.OverdueCaseRow::departmentId,
                        OverdueDashboardResponse.OverdueCaseRow::department, row -> "Department"),
                options(rows, OverdueDashboardResponse.OverdueCaseRow::sectionId,
                        OverdueDashboardResponse.OverdueCaseRow::currentSection,
                        OverdueDashboardResponse.OverdueCaseRow::department),
                options(rows, OverdueDashboardResponse.OverdueCaseRow::currentOwnerId,
                        OverdueDashboardResponse.OverdueCaseRow::currentOwner,
                        OverdueDashboardResponse.OverdueCaseRow::currentSection),
                options(rows, OverdueDashboardResponse.OverdueCaseRow::categoryId,
                        OverdueDashboardResponse.OverdueCaseRow::category, row -> "Category"),
                options(rows, OverdueDashboardResponse.OverdueCaseRow::workflowStageId,
                        OverdueDashboardResponse.OverdueCaseRow::currentWorkflowStage,
                        row -> "Workflow stage"),
                rows.stream().map(OverdueDashboardResponse.OverdueCaseRow::priority).distinct()
                        .sorted().toList(),
                rows.stream().map(OverdueDashboardResponse.OverdueCaseRow::escalationLevel)
                        .filter(Objects::nonNull).distinct().sorted().toList());
    }

    private List<OverdueDashboardResponse.Option> options(
            List<OverdueDashboardResponse.OverdueCaseRow> rows,
            java.util.function.Function<OverdueDashboardResponse.OverdueCaseRow, UUID> id,
            java.util.function.Function<OverdueDashboardResponse.OverdueCaseRow, String> label,
            java.util.function.Function<OverdueDashboardResponse.OverdueCaseRow, String> secondary) {
        Map<UUID, OverdueDashboardResponse.Option> values = new LinkedHashMap<>();
        for (var row : rows) {
            UUID value = id.apply(row);
            if (value != null) {
                values.putIfAbsent(value, new OverdueDashboardResponse.Option(value.toString(),
                        safeLabel(label.apply(row)), safeLabel(secondary.apply(row))));
            }
        }
        return values.values().stream().sorted(Comparator.comparing(
                OverdueDashboardResponse.Option::label, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void validateOverdueQuery(OverdueDashboardQuery query) {
        if (query.fromDate() != null && query.toDate() != null
                && query.fromDate().isAfter(query.toDate())) {
            throw new ValidationException("Overdue fromDate cannot be after toDate");
        }
        if (query.minDaysOverdue() != null && query.minDaysOverdue() < 1) {
            throw new ValidationException("Minimum days overdue must be at least 1");
        }
        if (query.maxDaysOverdue() != null && query.maxDaysOverdue() < 1) {
            throw new ValidationException("Maximum days overdue must be at least 1");
        }
        if (query.minDaysOverdue() != null && query.maxDaysOverdue() != null
                && query.minDaysOverdue() > query.maxDaysOverdue()) {
            throw new ValidationException("Minimum days overdue cannot exceed maximum");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safeLabel(String value) {
        return value == null || value.isBlank() ? "Not Available" : value;
    }

    private record OverdueData(List<OverdueDashboardResponse.OverdueCaseRow> rows,
                               OverdueDashboardResponse.Thresholds thresholds) { }

    private ScopeResolution resolveScope(DashboardScopeType type, UUID requestedId,
                                         UUID currentUserId) {
        return switch (type) {
            case SELF -> userScope(type, currentUserId, "My Dashboard");
            case USER, OFFICER -> {
                if (requestedId == null) {
                    throw new ValidationException(type + " scope requires a selected user");
                }
                yield userScope(type, requestedId,
                        type == DashboardScopeType.OFFICER ? "Officer" : "User");
            }
            case SECTION -> {
                UUID sectionId = requestedId;
                if (sectionId == null) {
                    sectionId = identityFacade.findUser(currentUserId)
                            .map(IdentityFacade.UserRef::sectionId)
                            .orElse(null);
                }
                UUID finalSectionId = sectionId;
                IdentityFacade.SectionRef section = identityFacade.activeSections(null).stream()
                        .filter(item -> item.id().equals(finalSectionId))
                        .findFirst()
                        .orElseThrow(() -> new ValidationException(
                                "A valid active section is required"));
                yield new ScopeResolution(type, section.id(), section.name(),
                        null, List.of(section.id()), false);
            }
            case DEPARTMENT -> {
                UUID departmentId = requestedId;
                if (departmentId == null) {
                    departmentId = identityFacade.findUser(currentUserId)
                            .map(IdentityFacade.UserRef::departmentId)
                            .orElse(null);
                }
                String departmentName = identityFacade.activeDepartmentNames().get(departmentId);
                if (departmentId == null || departmentName == null) {
                    throw new ValidationException("A valid active department is required");
                }
                List<UUID> sectionIds = identityFacade.activeSections(departmentId).stream()
                        .map(IdentityFacade.SectionRef::id)
                        .toList();
                yield new ScopeResolution(type, departmentId, departmentName,
                        null, sectionIds, false);
            }
            case ORGANIZATION -> new ScopeResolution(type, null,
                    "Entire Organization", null, List.of(), true);
        };
    }

    private ScopeResolution userScope(DashboardScopeType type, UUID userId,
                                      String labelPrefix) {
        IdentityFacade.UserRef user = identityFacade.findUser(userId)
                .filter(IdentityFacade.UserRef::active)
                .orElseThrow(() -> new ValidationException("A valid active user is required"));
        List<UUID> sections = user.sectionId() == null
                ? List.of()
                : List.of(user.sectionId());
        String label = type == DashboardScopeType.SELF
                ? labelPrefix
                : labelPrefix + ": " + user.fullName();
        return new ScopeResolution(type, user.id(), label, user.id(), sections, false);
    }

    private ScopeTotals aggregate(ScopeResolution scope, LocalDate activityFrom,
                                  LocalDate activityTo, LocalDate today) {
        if (scope.organization()) {
            return fetchTotals(null, null, null, activityFrom, activityTo, today);
        }
        if (scope.ownerId() != null) {
            UUID registrySectionId = scope.sectionIds().isEmpty() ? null : scope.sectionIds().get(0);
            return fetchTotals(scope.ownerId(), null, registrySectionId,
                    activityFrom, activityTo, today);
        }
        return fetchTotalsBySections(
                scope.sectionIds(), activityFrom, activityTo, today);
    }

    private ScopeTotals fetchTotals(UUID ownerId, UUID dataSectionId,
                                    UUID registrySectionId,
                                    LocalDate activityFrom, LocalDate activityTo,
                                    LocalDate today) {
        return new ScopeTotals(
                correspondenceFacade.fileMetrics(ownerId, dataSectionId),
                correspondenceFacade.fileActivity(
                        ownerId, dataSectionId, activityFrom, activityTo),
                workflowEscalationFacade.taskMetrics(ownerId, dataSectionId, today),
                registryFacade.diariesReceivedToday(registrySectionId),
                registryFacade.diariesReceivedBetween(
                        activityFrom, activityTo, registrySectionId),
                correspondenceFacade.diaryLetterMetrics(ownerId, dataSectionId),
                workflowEscalationFacade.letterTaskMetrics(
                        ownerId, dataSectionId, today));
    }

    private ScopeTotals fetchTotalsBySections(
            List<UUID> sectionIds,
            LocalDate activityFrom,
            LocalDate activityTo,
            LocalDate today) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return ScopeTotals.empty();
        }
        return new ScopeTotals(
                correspondenceFacade.fileMetricsBySections(sectionIds),
                correspondenceFacade.fileActivityBySections(
                        sectionIds, activityFrom, activityTo),
                workflowEscalationFacade.taskMetricsBySections(sectionIds, today),
                registryFacade.diariesReceivedBetweenSections(
                        today, today, sectionIds),
                registryFacade.diariesReceivedBetweenSections(
                        activityFrom, activityTo, sectionIds),
                correspondenceFacade.diaryLetterMetricsBySections(sectionIds),
                workflowEscalationFacade.letterTaskMetricsBySections(
                        sectionIds, today));
    }

    private List<RecentFileRow> recentFiles(ScopeResolution scope,
                                            LocalDate fromDate,
                                            LocalDate toDate) {
        List<CorrespondenceFacade.RecentFile> files = new ArrayList<>();
        if (scope.organization()) {
            files.addAll(correspondenceFacade.recentFiles(
                    null, null, fromDate, toDate, RECENT_LIMIT));
        } else if (scope.ownerId() != null) {
            files.addAll(correspondenceFacade.recentFiles(
                    scope.ownerId(), null, fromDate, toDate, RECENT_LIMIT));
        } else {
            files.addAll(correspondenceFacade.recentFilesBySections(
                    scope.sectionIds(), fromDate, toDate, RECENT_LIMIT));
        }
        Comparator<CorrespondenceFacade.RecentFile> newestFirst = Comparator
                .comparing(CorrespondenceFacade.RecentFile::openedDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
        return files.stream()
                .filter(Objects::nonNull)
                .sorted(newestFirst)
                .limit(RECENT_LIMIT)
                .map(file -> RecentFileRow.builder()
                        .fileId(file.fileId())
                        .fileNumber(file.fileNumber())
                        .subject(file.subject())
                        .currentStatus(file.currentStatus())
                        .openedDate(file.openedDate())
                        .build())
                .toList();
    }

    /**
     * Bucket overdue pending tasks by ageing threshold. Buckets are
     * cumulative-exclusive: a 20-day-overdue task counts once, in the
     * Escalation (>=15) bucket, not also in Reminder/Warning.
     */
    private SlaAgeing buildAgeing(ScopeResolution scope, LocalDate today,
                                  long pendingTotal) {
        List<WorkflowEscalationFacade.OverdueTask> overdue =
                workflowEscalationFacade.findOverdueTasks(today);

        long reminder = 0;
        long warning = 0;
        long escalation = 0;
        long highest = 0;
        long overdueInScope = 0;

        for (WorkflowEscalationFacade.OverdueTask task : overdue) {
            if (!inScope(task, scope)) {
                continue;
            }
            overdueInScope++;
            long days = task.daysOverdue();
            if (days >= 30) {
                highest++;
            } else if (days >= 15) {
                escalation++;
            } else if (days >= 7) {
                warning++;
            } else if (days >= 3) {
                reminder++;
            }
        }
        long onTrack = Math.max(pendingTotal - overdueInScope, 0);
        return SlaAgeing.builder()
                .onTrack(onTrack)
                .reminder(reminder)
                .warning(warning)
                .escalation(escalation)
                .highestEscalation(highest)
                .build();
    }

    private boolean inScope(WorkflowEscalationFacade.OverdueTask task,
                            ScopeResolution scope) {
        if (scope.organization()) {
            return true;
        }
        if (scope.ownerId() != null) {
            return scope.ownerId().equals(task.assignedToUserId());
        }
        return task.assignedToSectionId() != null
                && scope.sectionIds().contains(task.assignedToSectionId());
    }

    private List<IdentityFacade.EmployeeRef> employeesInScope(ScopeResolution scope) {
        return identityFacade.employees().stream()
                .filter(employee -> scope.organization()
                        || (scope.ownerId() != null && scope.ownerId().equals(employee.id()))
                        || (employee.sectionId() != null
                        && scope.sectionIds().contains(employee.sectionId())))
                .toList();
    }


    private List<DashboardScopeType> allowedScopes() {
        return List.of(DashboardScopeType.values()).stream()
                .filter(this::hasScopePermission)
                .toList();
    }

    private void requireScopePermission(DashboardScopeType type) {
        if (!hasScopePermission(type)) {
            throw new ForbiddenException("Dashboard scope is not permitted: " + type);
        }
    }

    private boolean hasScopePermission(DashboardScopeType type) {
        if (type == DashboardScopeType.SELF
                && SecurityUtils.hasAuthority(LEGACY_VIEW_PERMISSION)) {
            return true;
        }
        return SecurityUtils.hasAuthority(SCOPE_PERMISSIONS.get(type));
    }

    private void validateDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("Dashboard fromDate cannot be after toDate");
        }
        if (fromDate != null && toDate != null
                && ChronoUnit.DAYS.between(fromDate, toDate) > MAX_ACTIVITY_RANGE_DAYS) {
            throw new ValidationException("Dashboard date range cannot exceed five years");
        }
    }

    private void validateReason(String reason) {
        if (reason != null && reason.trim().length() > 500) {
            throw new ValidationException("Dashboard view reason cannot exceed 500 characters");
        }
    }

    private void auditViewAs(ScopeResolution scope, String reason) {
        String safeReason = reason == null ? null : reason.trim();
        String value = "scopeType=" + scope.type()
                + ", scopeId=" + scope.id()
                + ", scopeLabel=" + scope.label()
                + (safeReason == null || safeReason.isBlank()
                    ? ""
                    : ", reason=" + safeReason);
        auditService.record(MODULE, "DASHBOARD_SCOPE", scope.id(),
                "VIEW_AS", null, value);
    }

    private String userContext(IdentityFacade.UserRef user,
                               Map<UUID, String> departments,
                               Map<UUID, String> sections) {
        String department = departments.get(user.departmentId());
        String section = sections.get(user.sectionId());
        if (department == null && section == null) {
            return "Active user";
        }
        if (department == null) {
            return section;
        }
        if (section == null) {
            return department;
        }
        return department + " / " + section;
    }

    private UUID currentUser() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }

    private record ScopeResolution(
            DashboardScopeType type,
            UUID id,
            String label,
            UUID ownerId,
            List<UUID> sectionIds,
            boolean organization) {
    }

    private record ScopeTotals(
            CorrespondenceFacade.FileMetrics files,
            CorrespondenceFacade.FileActivityMetrics fileActivity,
            WorkflowEscalationFacade.TaskMetrics tasks,
            long receivedToday,
            long activityReceived,
            CorrespondenceFacade.DiaryLetterMetrics diaryLetters,
            WorkflowEscalationFacade.TaskMetrics diaryTasks) {

        static ScopeTotals empty() {
            return new ScopeTotals(
                    new CorrespondenceFacade.FileMetrics(0, 0, 0, 0, 0, 0, 0, 0),
                    new CorrespondenceFacade.FileActivityMetrics(0, 0),
                    new WorkflowEscalationFacade.TaskMetrics(0, 0),
                    0,
                    0,
                    new CorrespondenceFacade.DiaryLetterMetrics(0, 0),
                    new WorkflowEscalationFacade.TaskMetrics(0, 0));
        }

    }
}
