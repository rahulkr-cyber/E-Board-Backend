package com.bor.eboard.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reusable dashboard scope and activity filter request.
 */
public record DashboardQuery(
        DashboardScopeType scopeType,
        UUID scopeId,
        LocalDate fromDate,
        LocalDate toDate,
        String reason,
        boolean viewAsAction) {

    public DashboardScopeType effectiveScopeType() {
        return scopeType == null ? DashboardScopeType.SELF : scopeType;
    }
}
