package com.bor.eboard.dms.repository;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.dms.dto.DmsDocumentSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentSearchResponse;
import com.bor.eboard.dms.security.DmsDocumentAccessScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Repository
public class DmsSearchQueryRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "uploadedat", "si.uploaded_at",
            "updatedat", "COALESCE(si.document_updated_at, si.uploaded_at)",
            "documentnumber", "si.document_number",
            "title", "LOWER(si.title)",
            "documenttype", "LOWER(si.document_type_name)",
            "status", "si.status",
            "filesize", "COALESCE(lv.file_size, 0)");

    private static final String FROM_CLAUSE = """
             FROM dms_search_index si
             LEFT JOIN org_departments dept ON dept.id = si.department_id
             LEFT JOIN org_sections sec ON sec.id = si.section_id
             LEFT JOIN LATERAL (
                 SELECT version.mime_type, version.file_size
                 FROM dms_document_versions version
                 WHERE version.document_id = si.document_id
                   AND version.deleted = FALSE
                 ORDER BY version.version_number DESC
                 LIMIT 1
             ) lv ON TRUE
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DmsSearchQueryRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public PageResponse<DmsDocumentSearchResponse> search(
            DmsDocumentSearchRequest request,
            DmsDocumentAccessScope scope) {
        StringBuilder where = new StringBuilder(" WHERE si.deleted = FALSE");
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        appendAccessScope(where, parameters, scope);
        if (hasText(request.keywords())) {
            where.append(" AND (si.search_vector @@ websearch_to_tsquery('simple', :keywords)")
                    .append(" OR LOWER(COALESCE(si.keywords_text, '')) LIKE :keywordLike")
                    .append(" OR LOWER(COALESCE(si.ocr_text, '')) LIKE :keywordLike)");
            parameters.addValue("keywords", request.keywords().trim());
            parameters.addValue("keywordLike", like(request.keywords()));
        }
        if (hasText(request.documentNumber())) {
            where.append(" AND LOWER(si.document_number) LIKE :documentNumber");
            parameters.addValue("documentNumber", like(request.documentNumber()));
        }
        if (request.documentTypeId() != null) {
            where.append(" AND si.document_type_id = :documentTypeId");
            parameters.addValue("documentTypeId", request.documentTypeId());
        }
        if (request.uploadedBy() != null) {
            where.append(" AND si.uploaded_by = :uploadedBy");
            parameters.addValue("uploadedBy", request.uploadedBy());
        }
        if (request.departmentId() != null) {
            where.append(" AND si.department_id = :departmentId");
            parameters.addValue("departmentId", request.departmentId());
        }
        if (request.sectionId() != null) {
            where.append(" AND si.section_id = :sectionId");
            parameters.addValue("sectionId", request.sectionId());
        }
        if (hasText(request.status())) {
            where.append(" AND si.status = :status");
            parameters.addValue("status", request.status().trim().toUpperCase(Locale.ROOT));
        }
        if (request.uploadedFrom() != null) {
            where.append(" AND si.uploaded_at >= :uploadedFrom");
            parameters.addValue("uploadedFrom", request.uploadedFrom());
        }
        if (request.uploadedTo() != null) {
            where.append(" AND si.uploaded_at <= :uploadedTo");
            parameters.addValue("uploadedTo", request.uploadedTo());
        }
        if (request.modifiedFrom() != null) {
            where.append(" AND COALESCE(si.document_updated_at, si.uploaded_at) >= :modifiedFrom");
            parameters.addValue("modifiedFrom", request.modifiedFrom());
        }
        if (request.modifiedTo() != null) {
            where.append(" AND COALESCE(si.document_updated_at, si.uploaded_at) <= :modifiedTo");
            parameters.addValue("modifiedTo", request.modifiedTo());
        }
        if (request.minFileSizeBytes() != null) {
            where.append(" AND lv.file_size >= :minFileSizeBytes");
            parameters.addValue("minFileSizeBytes", request.minFileSizeBytes());
        }
        if (request.maxFileSizeBytes() != null) {
            where.append(" AND lv.file_size <= :maxFileSizeBytes");
            parameters.addValue("maxFileSizeBytes", request.maxFileSizeBytes());
        }

        List<String> tags = request.tags() == null
                ? List.of()
                : request.tags().stream().filter(this::hasText).map(String::trim).toList();
        for (int tagIndex = 0; tagIndex < tags.size(); tagIndex++) {
            String parameter = "tag" + tagIndex;
            where.append(" AND EXISTS (SELECT 1 FROM jsonb_array_elements_text(si.tags_json) tag_value ")
                    .append("WHERE LOWER(tag_value) = :").append(parameter).append(")");
            parameters.addValue(parameter, tags.get(tagIndex).toLowerCase(Locale.ROOT));
        }

        int metadataIndex = 0;
        if (request.metadata() != null) {
            for (Map.Entry<String, Object> entry : request.metadata().entrySet()) {
                if (!hasText(entry.getKey()) || isEmpty(entry.getValue())) {
                    continue;
                }
                String keyParameter = "metadataKey" + metadataIndex;
                parameters.addValue(keyParameter, entry.getKey().trim());
                Object value = entry.getValue();
                if (value instanceof Collection<?> collection) {
                    int itemIndex = 0;
                    for (Object item : collection) {
                        if (isEmpty(item)) {
                            continue;
                        }
                        String valueParameter = "metadataJson" + metadataIndex + "_" + itemIndex++;
                        where.append(" AND COALESCE(si.metadata_json -> :")
                                .append(keyParameter)
                                .append(", '[]'::jsonb) @> CAST(:")
                                .append(valueParameter)
                                .append(" AS jsonb)");
                        parameters.addValue(valueParameter, writeJson(List.of(item)));
                    }
                } else if (value instanceof Boolean || value instanceof Number) {
                    String valueParameter = "metadataExact" + metadataIndex;
                    where.append(" AND LOWER(COALESCE(jsonb_extract_path_text(si.metadata_json, :")
                            .append(keyParameter)
                            .append("), '')) = :")
                            .append(valueParameter);
                    parameters.addValue(valueParameter,
                            String.valueOf(value).toLowerCase(Locale.ROOT));
                } else {
                    String valueParameter = "metadataValue" + metadataIndex;
                    where.append(" AND LOWER(COALESCE(jsonb_extract_path_text(si.metadata_json, :")
                            .append(keyParameter)
                            .append("), '')) LIKE :")
                            .append(valueParameter);
                    parameters.addValue(valueParameter, like(String.valueOf(value)));
                }
                metadataIndex++;
            }
        }

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)" + FROM_CLAUSE + where,
                parameters,
                Long.class);

        String sortColumn = SORT_COLUMNS.getOrDefault(
                normalizeSort(request.sortBy()), "si.uploaded_at");
        String direction = "ASC".equalsIgnoreCase(request.sortDirection()) ? "ASC" : "DESC";
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 20 : request.size();
        parameters.addValue("limit", size);
        parameters.addValue("offset", (long) page * size);

        String select = """
                SELECT si.document_id,
                       si.document_number,
                       si.document_type_id,
                       si.document_type_code,
                       si.document_type_name,
                       si.title,
                       si.description,
                       si.status,
                       si.current_version_number,
                       si.uploaded_by,
                       si.uploaded_by_name,
                       si.department_id,
                       dept.name AS department_name,
                       si.section_id,
                       sec.name AS section_name,
                       si.uploaded_at,
                       si.document_updated_at,
                       si.metadata_json,
                       si.tags_json,
                       si.latest_file_name,
                       lv.mime_type AS latest_mime_type,
                       lv.file_size AS latest_file_size
                """ + FROM_CLAUSE + where
                + " ORDER BY " + sortColumn + " " + direction
                + ", si.document_number ASC LIMIT :limit OFFSET :offset";

        List<DmsDocumentSearchResponse> content = jdbcTemplate.query(
                select, parameters, this::mapRow);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return PageResponse.<DmsDocumentSearchResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .last(page + 1 >= totalPages)
                .build();
    }

    private void appendAccessScope(
            StringBuilder where,
            MapSqlParameterSource parameters,
            DmsDocumentAccessScope scope) {
        if (scope.administrator()) {
            return;
        }
        parameters.addValue("accessUserId", scope.userId());
        where.append(" AND (si.uploaded_by = :accessUserId OR EXISTS (")
                .append("SELECT 1 FROM dms_document_permissions dap ")
                .append("WHERE dap.document_id = si.document_id ")
                .append("AND dap.deleted = FALSE AND dap.active = TRUE ")
                .append("AND (dap.expires_at IS NULL OR dap.expires_at > CURRENT_TIMESTAMP) ")
                .append("AND dap.access_level = 'VIEW' AND (")
                .append("(dap.principal_type = 'USER' AND dap.principal_id = :accessUserId)");
        if (scope.departmentId() != null) {
            where.append(" OR (dap.principal_type = 'DEPARTMENT' AND dap.principal_id = :accessDepartmentId)");
            parameters.addValue("accessDepartmentId", scope.departmentId());
        }
        if (scope.sectionId() != null) {
            where.append(" OR (dap.principal_type = 'SECTION' AND dap.principal_id = :accessSectionId)");
            parameters.addValue("accessSectionId", scope.sectionId());
        }
        if (scope.roleIds() != null && !scope.roleIds().isEmpty()) {
            where.append(" OR (dap.principal_type = 'ROLE' AND dap.principal_id IN (:accessRoleIds))");
            parameters.addValue("accessRoleIds", scope.roleIds());
        }
        where.append(")))");
    }

    private DmsDocumentSearchResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DmsDocumentSearchResponse(
                rs.getObject("document_id", UUID.class),
                rs.getString("document_number"),
                rs.getObject("document_type_id", UUID.class),
                rs.getString("document_type_code"),
                rs.getString("document_type_name"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getInt("current_version_number"),
                rs.getObject("uploaded_by", UUID.class),
                rs.getString("uploaded_by_name"),
                rs.getObject("department_id", UUID.class),
                rs.getString("department_name"),
                rs.getObject("section_id", UUID.class),
                rs.getString("section_name"),
                toLocalDateTime(rs, "uploaded_at"),
                toLocalDateTime(rs, "document_updated_at"),
                readMap(rs.getString("metadata_json")),
                readList(rs.getString("tags_json")),
                rs.getString("latest_file_name"),
                rs.getString("latest_mime_type"),
                getLong(rs, "latest_file_size"));
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private Map<String, Object> readMap(String json) {
        if (!hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return Collections.unmodifiableMap(objectMapper.readValue(
                    json, new TypeReference<LinkedHashMap<String, Object>>() { }));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored DMS search metadata is invalid", ex);
        }
    }

    private List<String> readList(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(
                    json, new TypeReference<List<String>>() { }));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored DMS search tags are invalid", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("DMS search filter cannot be serialized", ex);
        }
    }

    private String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeSort(String value) {
        if (!hasText(value)) {
            return "uploadedat";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence text) {
            return text.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }
}
