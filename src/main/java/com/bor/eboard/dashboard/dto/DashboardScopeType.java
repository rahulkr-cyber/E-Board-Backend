package com.bor.eboard.dashboard.dto;

/**
 * Supported dashboard data scopes. Authentication, roles and JWT remain
 * unchanged; this enum controls only the data boundary used by dashboard
 * queries.
 */
public enum DashboardScopeType {
    SELF,
    SECTION,
    DEPARTMENT,
    OFFICER,
    USER,
    ORGANIZATION
}
