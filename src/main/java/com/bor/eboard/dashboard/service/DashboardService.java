package com.bor.eboard.dashboard.service;

import com.bor.eboard.dashboard.dto.DashboardMetrics;
import com.bor.eboard.dashboard.dto.DashboardQuery;
import com.bor.eboard.dashboard.dto.DashboardScopeOptionsResponse;
import com.bor.eboard.dashboard.dto.DashboardScopeType;
import com.bor.eboard.dashboard.dto.OverdueDashboardQuery;
import com.bor.eboard.dashboard.dto.OverdueDashboardResponse;
import com.bor.eboard.dashboard.dto.SlaTimelineEvent;

import java.util.UUID;

/**
 * Composes one reusable dashboard from Registry, Correspondence and Workflow
 * facades. The selected scope changes only the data boundary; authentication,
 * JWT, roles and permissions never change.
 */
public interface DashboardService {

    DashboardMetrics get(DashboardQuery query);

    DashboardScopeOptionsResponse scopeOptions(DashboardScopeType scopeType,
                                                UUID parentId,
                                                String query);

    /** Commissioner/senior-officer read-only overdue monitoring. */
    OverdueDashboardResponse overdue(OverdueDashboardQuery query);

    /** Unpaged rows reused by the existing Reports service. */
    java.util.List<OverdueDashboardResponse.OverdueCaseRow> overdueCasesForReport(
            OverdueDashboardQuery query);

    java.util.List<SlaTimelineEvent> overdueTimeline(UUID taskId);

    /** Backward-compatible personal dashboard endpoint. */
    DashboardMetrics personal();

    /** Backward-compatible section dashboard endpoint. */
    DashboardMetrics section();

    /** Backward-compatible leadership/admin endpoint. */
    DashboardMetrics global(String scopeLabel);
}
