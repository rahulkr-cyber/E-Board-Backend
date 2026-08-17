package com.bor.eboard.dashboard.dto;

import java.util.UUID;

/** Human-readable dashboard scope target; ids stay internal to API calls. */
public record DashboardScopeOption(
        UUID id,
        String label,
        String secondaryLabel) {
}
