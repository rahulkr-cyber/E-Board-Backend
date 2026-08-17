package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class DatabaseQueryMasterSourceResolver implements DmsMasterSourceResolver {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DmsMasterParameterBinder parameterBinder;
    private final DmsMasterOptionMapper optionMapper;

    public DatabaseQueryMasterSourceResolver(
            NamedParameterJdbcTemplate jdbcTemplate,
            DmsMasterParameterBinder parameterBinder,
            DmsMasterOptionMapper optionMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.parameterBinder = parameterBinder;
        this.optionMapper = optionMapper;
    }

    @Override
    public Set<DmsMasterSourceType> supportedTypes() {
        return Set.of(DmsMasterSourceType.DATABASE_QUERY);
    }

    @Override
    public List<DmsMasterDataOptionResponse> resolve(
            DmsMasterSource source,
            List<DmsMasterSourceParameter> parameterDefinitions,
            Map<String, Object> parameters) {
        String query = source.getQueryText();
        validateReadOnlyQuery(query);
        List<DmsMasterParameterBinder.BoundParameter> bound =
                parameterBinder.bind(parameterDefinitions, parameters);
        Map<String, Object> sqlParameters =
                parameterBinder.byTarget(bound, DmsMasterParameterLocation.SQL);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, sqlParameters);
        return optionMapper.mapRows(source, rows);
    }

    private void validateReadOnlyQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Database query is required for this master source");
        }
        String normalized = query.stripLeading().toLowerCase(Locale.ROOT);
        if (!(normalized.startsWith("select ") || normalized.startsWith("with "))
                || normalized.contains(";")
                || normalized.contains("--")
                || normalized.contains("/*")) {
            throw new ValidationException(
                    "DMS database master sources support one read-only SELECT/WITH statement");
        }
    }
}
