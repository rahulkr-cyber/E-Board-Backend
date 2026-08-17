package com.bor.eboard.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Filters for the Commissioner's read-only overdue monitoring view. */
public record OverdueDashboardQuery(
        UUID departmentId,
        UUID sectionId,
        UUID officerId,
        UUID categoryId,
        UUID workflowStageId,
        String priority,
        String escalationLevel,
        LocalDate fromDate,
        LocalDate toDate,
        Integer minDaysOverdue,
        Integer maxDaysOverdue,
        int page,
        int size,
        String sortBy,
        String sortDir) {

    public int safePage() { return Math.max(page, 0); }
    public int safeSize() { return Math.min(Math.max(size, 1), 100); }
    public String safeSortBy() { return sortBy == null || sortBy.isBlank() ? "daysOverdue" : sortBy; }
    public boolean ascending() { return "asc".equalsIgnoreCase(sortDir); }
}
