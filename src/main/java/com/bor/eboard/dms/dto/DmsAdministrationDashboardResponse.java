package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DmsAdministrationDashboardResponse(
        long totalDocuments,
        long activeDocuments,
        long archivedDocuments,
        long uploadedToday,
        long uploadedThisWeek,
        long uploadedThisMonth,
        long totalVersions,
        long totalStorageBytes,
        long activeDocumentTypes,
        long activeMetadataFields,
        long activeMasterSources,
        long activeShares,
        long auditEvents,
        long indexedDocuments,
        long activeUsersLast30Days,
        long inactiveDocuments,
        long searchesToday,
        long searchesThisWeek,
        long documentsMissingMandatoryMetadata,
        double metadataCompletionPercent,
        List<Metric> documentsByType,
        List<Metric> documentsByDepartment,
        List<Metric> documentsBySection,
        List<Metric> mostViewedDocuments,
        List<Metric> mostDownloadedDocuments,
        List<RecentDocument> recentUploads,
        List<RecentActivity> recentActivities,
        StorageHealthResponse storage,
        LocalDateTime generatedAt) {

    public record Metric(String label, long value) {
    }

    public record RecentDocument(
            UUID id,
            String documentNumber,
            String title,
            String documentTypeName,
            String departmentName,
            String sectionName,
            String uploadedByName,
            LocalDateTime uploadedAt) {
    }

    public record RecentActivity(
            String action,
            String documentNumber,
            String title,
            String actorName,
            boolean success,
            LocalDateTime createdAt) {
    }
}
