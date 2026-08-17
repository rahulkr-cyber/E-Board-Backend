package com.bor.eboard.dashboard.dto;

import com.bor.eboard.checklist.dto.ChecklistDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A role-scoped operational dashboard: headline widget counts, an SLA
 * ageing breakdown, and recent-file/task lists. The same shape backs every
 * role dashboard; the scope (self, section, or global) differs by endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

    private String scope;               // SELF | SECTION | DEPARTMENT | OFFICER | USER | ORGANIZATION
    private UUID scopeId;
    private String scopeLabel;

    // Activity filter applied to range-aware widgets and recent activity.
    private LocalDate activityFrom;
    private LocalDate activityTo;
    private long activityReceived;
    private long activityOpened;
    private long activityDisposed;

    // Headline widgets
    private long pendingFiles;
    private long overdueFiles;
    private long receivedToday;
    private long disposedThisMonth;
    private long returnedFiles;
    private long pendingApprovals;
    private long rejectedFiles;
    private long trackingDiaries;
    private long overdueDiaries;
    private long disposedDiaries;

    // Posting, charge and retirement widgets
    private long currentPosting;
    private long additionalCharges;
    private long vacantSeats;
    private long joiningPending;
    private long relievingPending;
    private long transfersToday;
    private long upcomingRetirements;
    private long retiredEmployees;

    // SLA ageing buckets (by days a pending task is overdue)
    private SlaAgeing slaAgeing;
    private ChecklistDtos.DashboardSummary checklist;

    // Lists
    private List<RecentFileRow> recentFiles;
}
