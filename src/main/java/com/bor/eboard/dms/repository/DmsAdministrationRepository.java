package com.bor.eboard.dms.repository;

import com.bor.eboard.dms.dto.DmsAdministrationDashboardResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DmsAdministrationRepository {

    private final JdbcTemplate jdbcTemplate;

    public DmsAdministrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardMetrics loadDashboardMetrics() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM dms_documents WHERE deleted = FALSE) AS total_documents,
                    (SELECT COUNT(*) FROM dms_documents WHERE deleted = FALSE AND status = 'ACTIVE') AS active_documents,
                    (SELECT COUNT(*) FROM dms_documents WHERE deleted = FALSE AND status = 'ARCHIVED') AS archived_documents,
                    (SELECT COUNT(*) FROM dms_documents WHERE deleted = FALSE AND uploaded_at >= CURRENT_DATE) AS uploaded_today,
                    (SELECT COUNT(*) FROM dms_documents WHERE deleted = FALSE
                        AND uploaded_at >= DATE_TRUNC('week', CURRENT_DATE)) AS uploaded_this_week,
                    (SELECT COUNT(*) FROM dms_documents WHERE deleted = FALSE
                        AND uploaded_at >= DATE_TRUNC('month', CURRENT_DATE)) AS uploaded_this_month,
                    (SELECT COUNT(*) FROM dms_document_versions WHERE deleted = FALSE) AS total_versions,
                    (SELECT COALESCE(SUM(file_size), 0) FROM dms_document_versions WHERE deleted = FALSE) AS total_storage_bytes,
                    (SELECT COUNT(*) FROM dms_document_types WHERE deleted = FALSE AND active = TRUE) AS active_document_types,
                    (SELECT COUNT(*) FROM dms_metadata_fields WHERE deleted = FALSE AND active = TRUE) AS active_metadata_fields,
                    (SELECT COUNT(*) FROM dms_master_sources WHERE deleted = FALSE AND active = TRUE) AS active_master_sources,
                    (SELECT COUNT(*) FROM dms_document_shares
                        WHERE deleted = FALSE AND active = TRUE
                        AND (expires_at IS NULL OR expires_at > NOW())) AS active_shares,
                    (SELECT COUNT(*) FROM dms_document_audit) AS audit_events,
                    (SELECT COUNT(*) FROM dms_search_index WHERE deleted = FALSE) AS indexed_documents,
                    (SELECT COUNT(DISTINCT actor_id) FROM dms_document_audit
                        WHERE success = TRUE AND actor_id IS NOT NULL
                          AND created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days') AS active_users_30_days,
                    (SELECT COUNT(*) FROM dms_documents document
                        WHERE document.deleted = FALSE
                          AND document.uploaded_at < CURRENT_TIMESTAMP - INTERVAL '90 days'
                          AND NOT EXISTS (
                              SELECT 1 FROM dms_document_audit audit
                              WHERE audit.document_id = document.id
                                AND audit.success = TRUE
                                AND audit.action IN ('VIEW', 'SHARED_VIEW', 'DOWNLOAD')
                                AND audit.created_at >= CURRENT_TIMESTAMP - INTERVAL '90 days'
                          )) AS inactive_documents,
                    (SELECT COUNT(*) FROM dms_document_audit
                        WHERE entity_type = 'SEARCH' AND action = 'SEARCH' AND success = TRUE
                          AND created_at >= CURRENT_DATE) AS searches_today,
                    (SELECT COUNT(*) FROM dms_document_audit
                        WHERE entity_type = 'SEARCH' AND action = 'SEARCH' AND success = TRUE
                          AND created_at >= DATE_TRUNC('week', CURRENT_DATE)) AS searches_this_week
                """, (rs, rowNum) -> new DashboardMetrics(
                rs.getLong("total_documents"),
                rs.getLong("active_documents"),
                rs.getLong("archived_documents"),
                rs.getLong("uploaded_today"),
                rs.getLong("uploaded_this_week"),
                rs.getLong("uploaded_this_month"),
                rs.getLong("total_versions"),
                rs.getLong("total_storage_bytes"),
                rs.getLong("active_document_types"),
                rs.getLong("active_metadata_fields"),
                rs.getLong("active_master_sources"),
                rs.getLong("active_shares"),
                rs.getLong("audit_events"),
                rs.getLong("indexed_documents"),
                rs.getLong("active_users_30_days"),
                rs.getLong("inactive_documents"),
                rs.getLong("searches_today"),
                rs.getLong("searches_this_week")));
    }

    public List<DmsAdministrationDashboardResponse.Metric> loadDocumentsByType() {
        return loadMetrics("""
                SELECT dt.name AS label, COUNT(*) AS metric_value
                FROM dms_documents d
                JOIN dms_document_types dt ON dt.id = d.document_type_id
                WHERE d.deleted = FALSE
                GROUP BY dt.name
                ORDER BY metric_value DESC, dt.name ASC
                LIMIT 10
                """);
    }

    public List<DmsAdministrationDashboardResponse.Metric> loadDocumentsByDepartment() {
        return loadMetrics("""
                SELECT COALESCE(dept.name, 'Unassigned') AS label, COUNT(*) AS metric_value
                FROM dms_documents d
                LEFT JOIN org_departments dept ON dept.id = d.department_id
                WHERE d.deleted = FALSE
                GROUP BY COALESCE(dept.name, 'Unassigned')
                ORDER BY metric_value DESC, label ASC
                LIMIT 10
                """);
    }

    public List<DmsAdministrationDashboardResponse.Metric> loadDocumentsBySection() {
        return loadMetrics("""
                SELECT COALESCE(sec.name, 'Unassigned') AS label, COUNT(*) AS metric_value
                FROM dms_documents d
                LEFT JOIN org_sections sec ON sec.id = d.section_id
                WHERE d.deleted = FALSE
                GROUP BY COALESCE(sec.name, 'Unassigned')
                ORDER BY metric_value DESC, label ASC
                LIMIT 10
                """);
    }

    public List<DmsAdministrationDashboardResponse.Metric> loadMostViewedDocuments() {
        return loadDocumentActivityMetrics(List.of("VIEW", "SHARED_VIEW"));
    }

    public List<DmsAdministrationDashboardResponse.Metric> loadMostDownloadedDocuments() {
        return loadDocumentActivityMetrics(List.of("DOWNLOAD"));
    }

    public List<DmsAdministrationDashboardResponse.RecentDocument> loadRecentUploads() {
        return jdbcTemplate.query("""
                SELECT si.document_id,
                       si.document_number,
                       si.title,
                       si.document_type_name,
                       dept.name AS department_name,
                       sec.name AS section_name,
                       si.uploaded_by_name,
                       si.uploaded_at
                FROM dms_search_index si
                LEFT JOIN org_departments dept ON dept.id = si.department_id
                LEFT JOIN org_sections sec ON sec.id = si.section_id
                WHERE si.deleted = FALSE
                ORDER BY si.uploaded_at DESC
                LIMIT 8
                """, (rs, rowNum) -> new DmsAdministrationDashboardResponse.RecentDocument(
                rs.getObject("document_id", java.util.UUID.class),
                rs.getString("document_number"),
                rs.getString("title"),
                rs.getString("document_type_name"),
                rs.getString("department_name"),
                rs.getString("section_name"),
                rs.getString("uploaded_by_name"),
                rs.getTimestamp("uploaded_at").toLocalDateTime()));
    }

    public List<DmsAdministrationDashboardResponse.RecentActivity> loadRecentActivities() {
        return jdbcTemplate.query("""
                SELECT audit.action,
                       search.document_number,
                       search.title,
                       COALESCE(audit.actor_name, 'System') AS actor_name,
                       audit.success,
                       audit.created_at
                FROM dms_document_audit audit
                LEFT JOIN dms_search_index search ON search.document_id = audit.document_id
                ORDER BY audit.created_at DESC
                LIMIT 10
                """, (rs, rowNum) -> new DmsAdministrationDashboardResponse.RecentActivity(
                rs.getString("action"),
                rs.getString("document_number"),
                rs.getString("title"),
                rs.getString("actor_name"),
                rs.getBoolean("success"),
                rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public long countDocumentsMissingMandatoryMetadata() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dms_documents document
                WHERE document.deleted = FALSE
                  AND EXISTS (
                      SELECT 1
                      FROM dms_metadata_fields field
                      WHERE field.document_type_id = document.document_type_id
                        AND field.deleted = FALSE
                        AND field.active = TRUE
                        AND field.required = TRUE
                        AND NOT EXISTS (
                            SELECT 1
                            FROM dms_document_metadata metadata
                            WHERE metadata.document_id = document.id
                              AND metadata.metadata_field_id = field.id
                              AND metadata.deleted = FALSE
                        )
                  )
                """, Long.class);
        return count == null ? 0L : count;
    }

    public boolean databaseAvailable() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1;
    }

    public long countDocumentTypesWithoutMetadata() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dms_document_types dt
                WHERE dt.deleted = FALSE
                  AND dt.active = TRUE
                  AND NOT EXISTS (
                      SELECT 1
                      FROM dms_metadata_fields mf
                      WHERE mf.document_type_id = dt.id
                        AND mf.deleted = FALSE
                        AND mf.active = TRUE
                  )
                """, Long.class);
        return count == null ? 0L : count;
    }

    public long countUnindexedDocuments() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dms_documents d
                WHERE d.deleted = FALSE
                  AND NOT EXISTS (
                      SELECT 1
                      FROM dms_search_index si
                      WHERE si.document_id = d.id
                        AND si.deleted = FALSE
                  )
                """, Long.class);
        return count == null ? 0L : count;
    }

    public long countInactiveReferencedMasterSources() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT mf.master_source_id)
                FROM dms_metadata_fields mf
                JOIN dms_master_sources ms ON ms.id = mf.master_source_id
                WHERE mf.deleted = FALSE
                  AND mf.active = TRUE
                  AND mf.master_source_id IS NOT NULL
                  AND (ms.deleted = TRUE OR ms.active = FALSE)
                """, Long.class);
        return count == null ? 0L : count;
    }

    private List<DmsAdministrationDashboardResponse.Metric> loadDocumentActivityMetrics(
            List<String> actions) {
        String placeholders = String.join(", ", actions.stream().map(value -> "?").toList());
        String sql = """
                SELECT search.document_number || ' · ' || search.title AS label,
                       COUNT(*) AS metric_value
                FROM dms_document_audit audit
                JOIN dms_search_index search ON search.document_id = audit.document_id
                WHERE audit.success = TRUE
                  AND audit.action IN (%s)
                  AND audit.created_at >= CURRENT_TIMESTAMP - INTERVAL '90 days'
                  AND search.deleted = FALSE
                GROUP BY search.document_number, search.title
                ORDER BY metric_value DESC, search.document_number ASC
                LIMIT 8
                """.formatted(placeholders);
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new DmsAdministrationDashboardResponse.Metric(
                        rs.getString("label"), rs.getLong("metric_value")),
                actions.toArray());
    }

    private List<DmsAdministrationDashboardResponse.Metric> loadMetrics(String sql) {
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new DmsAdministrationDashboardResponse.Metric(
                        rs.getString("label"), rs.getLong("metric_value")));
    }

    public record DashboardMetrics(
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
            long searchesThisWeek) {
    }
}
