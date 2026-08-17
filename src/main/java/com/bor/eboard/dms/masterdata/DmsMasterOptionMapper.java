package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DmsMasterOptionMapper {

    public List<DmsMasterDataOptionResponse> mapRows(
            DmsMasterSource source,
            List<Map<String, Object>> rows) {
        int limit = source.getMaxResults() == null ? 500 : source.getMaxResults();
        return rows.stream()
                .limit(limit)
                .map(row -> mapRow(source, row))
                .toList();
    }

    private DmsMasterDataOptionResponse mapRow(
            DmsMasterSource source,
            Map<String, Object> row) {
        Object value = getIgnoreCase(row, source.getValueField());
        Object label = getIgnoreCase(row, source.getLabelField());
        if (value == null || label == null) {
            throw new BusinessException(
                    "Master source result does not contain configured value/label fields");
        }
        return new DmsMasterDataOptionResponse(
                String.valueOf(value),
                String.valueOf(label),
                new LinkedHashMap<>(row));
    }

    private Object getIgnoreCase(Map<String, Object> row, String field) {
        if (row.containsKey(field)) {
            return row.get(field);
        }
        return row.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(field))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
