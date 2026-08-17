package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class StoredProcedureMasterSourceResolver implements DmsMasterSourceResolver {

    private static final Pattern PROCEDURE_NAME =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$");

    private final JdbcTemplate jdbcTemplate;
    private final DmsMasterParameterBinder parameterBinder;
    private final DmsMasterOptionMapper optionMapper;

    public StoredProcedureMasterSourceResolver(
            JdbcTemplate jdbcTemplate,
            DmsMasterParameterBinder parameterBinder,
            DmsMasterOptionMapper optionMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.parameterBinder = parameterBinder;
        this.optionMapper = optionMapper;
    }

    @Override
    public Set<DmsMasterSourceType> supportedTypes() {
        return Set.of(DmsMasterSourceType.STORED_PROCEDURE);
    }

    @Override
    public List<DmsMasterDataOptionResponse> resolve(
            DmsMasterSource source,
            List<DmsMasterSourceParameter> parameterDefinitions,
            Map<String, Object> parameters) {
        String procedure = source.getProcedureName();
        if (procedure == null || !PROCEDURE_NAME.matcher(procedure).matches()) {
            throw new ValidationException("Stored procedure name is invalid");
        }

        String schema = null;
        String name = procedure;
        int separator = procedure.indexOf('.');
        if (separator > 0) {
            schema = procedure.substring(0, separator);
            name = procedure.substring(separator + 1);
        }

        List<DmsMasterParameterBinder.BoundParameter> bound =
                parameterBinder.bind(parameterDefinitions, parameters);
        Map<String, Object> input = parameterBinder.byTarget(bound, DmsMasterParameterLocation.SQL);

        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName(name);
        if (schema != null) {
            call = call.withSchemaName(schema);
        }
        Map<String, Object> output = call.execute(input);
        List<Map<String, Object>> rows = firstResultSet(output);
        return optionMapper.mapRows(source, rows);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> firstResultSet(Map<String, Object> output) {
        for (Object value : output.values()) {
            if (value instanceof List<?> list) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        rows.add((Map<String, Object>) map);
                    }
                }
                if (!rows.isEmpty()) {
                    return rows;
                }
            }
        }
        throw new BusinessException("Stored procedure did not return a tabular result set");
    }
}
