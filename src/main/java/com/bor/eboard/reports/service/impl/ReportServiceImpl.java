package com.bor.eboard.reports.service.impl;

import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.dashboard.dto.OverdueDashboardQuery;
import com.bor.eboard.dashboard.service.DashboardService;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.registry.facade.RegistryFacade;
import com.bor.eboard.reports.dto.ReportColumn;
import com.bor.eboard.reports.dto.ReportData;
import com.bor.eboard.reports.dto.ReportRequest;
import com.bor.eboard.reports.service.ReportService;
import com.bor.eboard.workflow.facade.WorkflowEscalationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assembles each report's columns, rows, filter summary and totals.
 * Data is pulled exclusively through module facades, so the Reports module
 * never touches another module's repositories (02_ARCHITECTURE.md sec 25).
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final RegistryFacade registryFacade;
    private final CorrespondenceFacade correspondenceFacade;
    private final WorkflowEscalationFacade workflowEscalationFacade;
    private final IdentityFacade identityFacade;
    private final DashboardService dashboardService;

    @Override
    @Transactional(readOnly = true)
    public ReportData build(String reportCode, ReportRequest request) {
        ReportRequest req = request != null ? request : new ReportRequest();
        return switch (reportCode) {
            case "diary-register" -> diaryRegister(req);
            case "dispatch-register" -> dispatchRegister(req);
            case "pending" -> pending(req);
            case "disposed" -> disposed(req);
            case "monthly-pocket" -> monthlyPocket(req);
            case "overdue-monitoring" -> overdueMonitoring(req);
            default -> throw new ValidationException("Unknown report code: " + reportCode);
        };
    }

    // ------------------------------------------------------------------
    // Diary Register (11_REPORTS.md section 2)
    // ------------------------------------------------------------------

    private ReportData diaryRegister(ReportRequest req) {
        List<ReportColumn> columns = List.of(
                new ReportColumn("sno", "S.No", 1),
                new ReportColumn("diaryNumber", "Diary No.", 3),
                new ReportColumn("receivedDate", "Received", 3),
                new ReportColumn("senderName", "Sender", 3),
                new ReportColumn("senderDepartment", "Sender Dept.", 3),
                new ReportColumn("letterNumber", "Letter No.", 3),
                new ReportColumn("subject", "Subject", 5),
                new ReportColumn("categoryName", "Category", 2),
                new ReportColumn("priorityName", "Priority", 2),
                new ReportColumn("sectionName", "Section", 3),
                new ReportColumn("status", "Status", 2));

        List<Map<String, Object>> rows = new ArrayList<>();
        int sno = 1;
        for (RegistryFacade.DiaryReportRow r : registryFacade.diaryRegisterReport(
                req.getFromDate(), req.getToDate(), req.getSectionId(), req.getStatus())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sno", sno++);
            row.put("diaryNumber", r.diaryNumber());
            row.put("receivedDate", fmt(r.receivedDate()));
            row.put("senderName", nvl(r.senderName()));
            row.put("senderDepartment", nvl(r.senderDepartment()));
            row.put("letterNumber", nvl(r.letterNumber()));
            row.put("subject", nvl(r.subject()));
            row.put("categoryName", nvl(r.categoryName()));
            row.put("priorityName", nvl(r.priorityName()));
            row.put("sectionName", nvl(r.sectionName()));
            row.put("status", r.status());
            rows.add(row);
        }
        return assemble("diary-register", "Diary Register", columns, rows,
                diaryFilters(req), totalCount(rows.size(), "sno"));
    }

    // ------------------------------------------------------------------
    // Dispatch Register (section 3)
    // ------------------------------------------------------------------

    private ReportData dispatchRegister(ReportRequest req) {
        List<ReportColumn> columns = List.of(
                new ReportColumn("sno", "S.No", 1),
                new ReportColumn("dispatchNumber", "Dispatch No.", 3),
                new ReportColumn("dispatchDate", "Date", 2),
                new ReportColumn("letterNumber", "Letter No.", 3),
                new ReportColumn("fileNumber", "File No.", 3),
                new ReportColumn("recipientName", "Recipient", 3),
                new ReportColumn("recipientDepartment", "Recipient Dept.", 3),
                new ReportColumn("dispatchMode", "Mode", 2),
                new ReportColumn("trackingNumber", "Tracking", 2),
                new ReportColumn("status", "Status", 2));

        List<Map<String, Object>> rows = new ArrayList<>();
        int sno = 1;
        for (CorrespondenceFacade.DispatchReportRow r : correspondenceFacade.dispatchReport(
                req.getFromDate(), req.getToDate(), null, null, req.getStatus())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sno", sno++);
            row.put("dispatchNumber", r.dispatchNumber());
            row.put("dispatchDate", fmt(r.dispatchDate()));
            row.put("letterNumber", nvl(r.letterNumber()));
            row.put("fileNumber", nvl(r.fileNumber()));
            row.put("recipientName", nvl(r.recipientName()));
            row.put("recipientDepartment", nvl(r.recipientDepartment()));
            row.put("dispatchMode", nvl(r.dispatchMode()));
            row.put("trackingNumber", nvl(r.trackingNumber()));
            row.put("status", r.status());
            rows.add(row);
        }
        return assemble("dispatch-register", "Dispatch Register", columns, rows,
                dateFilters(req), totalCount(rows.size(), "sno"));
    }

    // ------------------------------------------------------------------
    // Pending Report (section 4)
    // ------------------------------------------------------------------

    private ReportData pending(ReportRequest req) {
        List<ReportColumn> columns = List.of(
                new ReportColumn("sno", "S.No", 1),
                new ReportColumn("fileNumber", "File No.", 3),
                new ReportColumn("subject", "Subject", 5),
                new ReportColumn("currentOwnerName", "Owner", 3),
                new ReportColumn("currentSectionName", "Section", 3),
                new ReportColumn("openedDate", "Pending Since", 2),
                new ReportColumn("daysPending", "Days", 1),
                new ReportColumn("priorityName", "Priority", 2),
                new ReportColumn("status", "Status", 2));

        List<Map<String, Object>> rows = new ArrayList<>();
        int sno = 1;
        List<CorrespondenceFacade.FileReportRow> data = correspondenceFacade.pendingReport(
                req.getSectionId(), req.getOfficerId(), req.getCategoryId(), req.getPriorityId());
        for (CorrespondenceFacade.FileReportRow r : data) {
            if (req.isOverdueOnly() && (r.daysPending() == null || r.daysPending() < 1)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sno", sno++);
            row.put("fileNumber", r.fileNumber());
            row.put("subject", nvl(r.subject()));
            row.put("currentOwnerName", nvl(r.currentOwnerName()));
            row.put("currentSectionName", nvl(r.currentSectionName()));
            row.put("openedDate", fmt(r.openedDate()));
            row.put("daysPending", r.daysPending() != null ? r.daysPending() : "");
            row.put("priorityName", nvl(r.priorityName()));
            row.put("status", r.status());
            rows.add(row);
        }
        return assemble("pending", "Pending Report", columns, rows,
                pendingFilters(req), totalCount(rows.size(), "sno"));
    }

    // ------------------------------------------------------------------
    // Disposed Report (section 5)
    // ------------------------------------------------------------------

    private ReportData disposed(ReportRequest req) {
        List<ReportColumn> columns = List.of(
                new ReportColumn("sno", "S.No", 1),
                new ReportColumn("fileNumber", "File No.", 3),
                new ReportColumn("subject", "Subject", 5),
                new ReportColumn("currentSectionName", "Section", 3),
                new ReportColumn("currentOwnerName", "Disposed By", 3),
                new ReportColumn("openedDate", "Opened", 2),
                new ReportColumn("closedDate", "Disposed", 2),
                new ReportColumn("processingDays", "Proc. Days", 2),
                new ReportColumn("status", "Status", 2));

        List<Map<String, Object>> rows = new ArrayList<>();
        int sno = 1;
        long totalProcessing = 0;
        int counted = 0;
        for (CorrespondenceFacade.FileReportRow r : correspondenceFacade.disposedReport(
                req.getFromDate(), req.getToDate(), req.getSectionId(),
                req.getOfficerId(), req.getCategoryId(), req.getPriorityId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sno", sno++);
            row.put("fileNumber", r.fileNumber());
            row.put("subject", nvl(r.subject()));
            row.put("currentSectionName", nvl(r.currentSectionName()));
            row.put("currentOwnerName", nvl(r.currentOwnerName()));
            row.put("openedDate", fmt(r.openedDate()));
            row.put("closedDate", fmt(r.closedDate()));
            row.put("processingDays", r.processingDays() != null ? r.processingDays() : "");
            row.put("status", r.status());
            rows.add(row);
            if (r.processingDays() != null) {
                totalProcessing += r.processingDays();
                counted++;
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("sno", "Total: " + rows.size());
        totals.put("processingDays", counted > 0
                ? "Avg " + (totalProcessing / counted) : "");
        return assemble("disposed", "Disposed Report", columns, rows,
                disposedFilters(req), totals);
    }

    // ------------------------------------------------------------------
    // Monthly Pocket (section 9) — consolidated per-section summary
    // ------------------------------------------------------------------

    private ReportData monthlyPocket(ReportRequest req) {
        int month = req.getMonth() != null ? req.getMonth() : LocalDate.now().getMonthValue();
        int year = req.getYear() != null ? req.getYear() : LocalDate.now().getYear();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<ReportColumn> columns = List.of(
                new ReportColumn("section", "Section", 4),
                new ReportColumn("received", "Received", 2),
                new ReportColumn("disposed", "Disposed", 2),
                new ReportColumn("pending", "Pending", 2),
                new ReportColumn("overdue", "Overdue", 2));

        // One row per section, plus a totals row.
        Map<UUID, String> sections = identityFacade.sectionNames();
        List<Map<String, Object>> rows = new ArrayList<>();
        long tReceived = 0;
        long tDisposed = 0;
        long tPending = 0;
        long tOverdue = 0;
        LocalDate today = LocalDate.now();

        for (Map.Entry<UUID, String> entry : sections.entrySet()) {
            UUID sectionId = entry.getKey();
            long received = countDisposedOpenedInRange(sectionId, monthStart, monthEnd, true);
            long disposed = countDisposedOpenedInRange(sectionId, monthStart, monthEnd, false);
            CorrespondenceFacade.FileMetrics fm = correspondenceFacade.fileMetrics(null, sectionId);
            long pending = fm.open() + fm.inProgress() + fm.pendingApproval() + fm.returned();
            long overdue = workflowEscalationFacade.taskMetrics(null, sectionId, today).overdue();
            if (received == 0 && disposed == 0 && pending == 0 && overdue == 0) {
                continue; // skip empty sections to keep the pocket compact
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("section", entry.getValue());
            row.put("received", received);
            row.put("disposed", disposed);
            row.put("pending", pending);
            row.put("overdue", overdue);
            rows.add(row);
            tReceived += received;
            tDisposed += disposed;
            tPending += pending;
            tOverdue += overdue;
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("section", "Total");
        totals.put("received", tReceived);
        totals.put("disposed", tDisposed);
        totals.put("pending", tPending);
        totals.put("overdue", tOverdue);

        List<String> filters = new ArrayList<>();
        filters.add("Month: " + String.format("%02d/%d", month, year));
        return assemble("monthly-pocket", "Monthly Pocket Report", columns, rows, filters, totals);
    }

    /** Helper: received = diaries received in range; disposed = files closed in range. */
    private long countDisposedOpenedInRange(UUID sectionId, LocalDate from, LocalDate to,
                                            boolean received) {
        if (received) {
            // Received in the month is approximated by diaries received today only at
            // count granularity; for the pocket we use the diary register report size.
            return registryFacade.diaryRegisterReport(from, to, sectionId, null).size();
        }
        return correspondenceFacade.disposedReport(from, to, sectionId, null, null, null).size();
    }


    // ------------------------------------------------------------------
    // Commissioner overdue monitoring report
    // ------------------------------------------------------------------

    private ReportData overdueMonitoring(ReportRequest req) {
        List<ReportColumn> columns = List.of(
                new ReportColumn("sno", "S.No", 1),
                new ReportColumn("priority", "Priority", 2),
                new ReportColumn("daysOverdue", "Days Overdue", 2),
                new ReportColumn("diaryNumber", "Diary No.", 3),
                new ReportColumn("letterNumber", "Letter No.", 3),
                new ReportColumn("fileNumber", "File No.", 3),
                new ReportColumn("subject", "Subject", 5),
                new ReportColumn("category", "Category", 3),
                new ReportColumn("workflowStage", "Workflow Stage", 3),
                new ReportColumn("currentOwner", "Current Owner", 3),
                new ReportColumn("section", "Section", 3),
                new ReportColumn("department", "Department", 3),
                new ReportColumn("assignedDate", "Assigned", 3),
                new ReportColumn("dueDate", "Due", 2),
                new ReportColumn("escalationLevel", "Escalation", 2),
                new ReportColumn("lastMovementDate", "Last Movement", 3));

        OverdueDashboardQuery query = new OverdueDashboardQuery(
                null, req.getSectionId(), req.getOfficerId(), req.getCategoryId(), null,
                null, null, req.getFromDate(), req.getToDate(), null, null,
                0, 100, "daysOverdue", "desc");
        var cases = dashboardService.overdueCasesForReport(query);
        List<Map<String, Object>> rows = new ArrayList<>();
        int sno = 1;
        long totalDelay = 0;
        long longestDelay = 0;
        for (var item : cases) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sno", sno++);
            row.put("priority", item.priority());
            row.put("daysOverdue", item.daysOverdue());
            row.put("diaryNumber", nvl(item.diaryNumber()));
            row.put("letterNumber", nvl(item.letterNumber()));
            row.put("fileNumber", nvl(item.fileNumber()));
            row.put("subject", nvl(item.subject()));
            row.put("category", nvl(item.category()));
            row.put("workflowStage", nvl(item.currentWorkflowStage()));
            row.put("currentOwner", nvl(item.currentOwner()));
            row.put("section", nvl(item.currentSection()));
            row.put("department", nvl(item.department()));
            row.put("assignedDate", fmt(item.assignedDate()));
            row.put("dueDate", fmt(item.dueDate()));
            row.put("escalationLevel", nvl(item.escalationLevel()));
            row.put("lastMovementDate", fmt(item.lastMovementDate()));
            rows.add(row);
            totalDelay += item.daysOverdue();
            longestDelay = Math.max(longestDelay, item.daysOverdue());
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("sno", "Total: " + rows.size());
        totals.put("priority", rows.isEmpty() ? "Average delay: 0" :
                String.format("Average delay: %.1f days", (double) totalDelay / rows.size()));
        totals.put("daysOverdue", "Longest: " + longestDelay + " days");
        return assemble("overdue-monitoring", "Overdue Monitoring Report", columns, rows,
                overdueFilters(req), totals);
    }

    private List<String> overdueFilters(ReportRequest req) {
        List<String> filters = dateFilters(req);
        if (req.getSectionId() != null) filters.add("Section: " + sectionName(req.getSectionId()));
        if (req.getOfficerId() != null) {
            filters.add("Officer: " + identityFacade.userNames()
                    .getOrDefault(req.getOfficerId(), req.getOfficerId().toString()));
        }
        if (req.getCategoryId() != null) filters.add("Category filter applied");
        return filters;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private ReportData assemble(String code, String title, List<ReportColumn> columns,
                                List<Map<String, Object>> rows, List<String> filters,
                                Map<String, Object> totals) {
        String generatedBy = SecurityUtils.getCurrentUserId()
                .map(id -> identityFacade.userNames().getOrDefault(id, id.toString()))
                .orElse("System");
        return ReportData.builder()
                .reportCode(code)
                .title(title)
                .columns(columns)
                .rows(rows)
                .filterSummary(filters)
                .totals(totals)
                .generatedByName(generatedBy)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Object> totalCount(int count, String firstKey) {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put(firstKey, "Total: " + count);
        return totals;
    }

    private List<String> dateFilters(ReportRequest req) {
        List<String> f = new ArrayList<>();
        if (req.getFromDate() != null) f.add("From: " + req.getFromDate().format(DATE));
        if (req.getToDate() != null) f.add("To: " + req.getToDate().format(DATE));
        if (req.getStatus() != null) f.add("Status: " + req.getStatus());
        return f;
    }

    private List<String> diaryFilters(ReportRequest req) {
        List<String> f = dateFilters(req);
        if (req.getSectionId() != null) f.add("Section: " + sectionName(req.getSectionId()));
        return f;
    }

    private List<String> pendingFilters(ReportRequest req) {
        List<String> f = new ArrayList<>();
        if (req.getSectionId() != null) f.add("Section: " + sectionName(req.getSectionId()));
        if (req.isOverdueOnly()) f.add("Overdue only");
        return f;
    }

    private List<String> disposedFilters(ReportRequest req) {
        List<String> f = dateFilters(req);
        if (req.getSectionId() != null) f.add("Section: " + sectionName(req.getSectionId()));
        return f;
    }

    private String sectionName(UUID sectionId) {
        return identityFacade.sectionNames().getOrDefault(sectionId, sectionId.toString());
    }

    private String fmt(LocalDate date) {
        return date != null ? date.format(DATE) : "";
    }

    private String fmt(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME) : "";
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
