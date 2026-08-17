package com.bor.eboard.dashboard.dto;

import com.bor.eboard.common.dto.PageResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Read-only Commissioner overdue dashboard payload. */
public record OverdueDashboardResponse(
        Counters counters,
        List<Breakdown> sectionWise,
        List<Breakdown> departmentWise,
        List<Breakdown> officerWise,
        List<Breakdown> categoryWise,
        FilterOptions filterOptions,
        Thresholds thresholds,
        PageResponse<OverdueCaseRow> cases) {

    public record Counters(long totalPending, long withinSla, long dueToday,
                           long dueTomorrow, long overdue, long criticalOverdue,
                           long overdueToday, long overdueThisWeek) { }

    public record Breakdown(String label, long count) { }

    public record Option(String id, String label, String secondaryLabel) { }

    public record FilterOptions(List<Option> departments, List<Option> sections,
                                List<Option> officers, List<Option> categories,
                                List<Option> workflowStages,
                                List<String> priorities,
                                List<String> escalationLevels) { }

    public record Thresholds(int highDays, int criticalDays) { }

    public record OverdueCaseRow(
            UUID taskId,
            UUID workflowInstanceId,
            UUID fileId,
            UUID letterId,
            UUID diaryEntryId,
            String diaryNumber,
            String letterNumber,
            String fileNumber,
            String subject,
            UUID categoryId,
            String category,
            UUID departmentId,
            String department,
            UUID sectionId,
            String currentSection,
            UUID currentOwnerId,
            String currentOwner,
            UUID workflowStageId,
            String currentWorkflowStage,
            String priority,
            String sourcePriority,
            String slaStatus,
            long daysOverdue,
            LocalDateTime assignedDate,
            LocalDate dueDate,
            LocalDateTime pendingSince,
            String escalationLevel,
            LocalDateTime lastMovementDate) { }
}
