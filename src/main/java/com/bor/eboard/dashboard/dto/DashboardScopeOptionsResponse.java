package com.bor.eboard.dashboard.dto;

import java.util.List;

/** Scope types permitted by RBAC and selectable targets for one scope type. */
public record DashboardScopeOptionsResponse(
        List<DashboardScopeType> allowedScopes,
        List<DashboardScopeOption> options) {
}
