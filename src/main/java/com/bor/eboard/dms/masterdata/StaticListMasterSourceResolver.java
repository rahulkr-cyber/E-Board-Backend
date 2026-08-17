package com.bor.eboard.dms.masterdata;

import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class StaticListMasterSourceResolver implements DmsMasterSourceResolver {

    private final DmsMasterConfigurationCodec configurationCodec;

    public StaticListMasterSourceResolver(DmsMasterConfigurationCodec configurationCodec) {
        this.configurationCodec = configurationCodec;
    }

    @Override
    public Set<DmsMasterSourceType> supportedTypes() {
        return Set.of(DmsMasterSourceType.STATIC_LIST);
    }

    @Override
    public List<DmsMasterDataOptionResponse> resolve(
            DmsMasterSource source,
            List<DmsMasterSourceParameter> parameterDefinitions,
            Map<String, Object> parameters) {
        int limit = source.getMaxResults() == null ? 500 : source.getMaxResults();
        return configurationCodec.staticOptions(source.getConfigurationJson()).stream()
                .filter(option -> !Boolean.FALSE.equals(option.get("active")))
                .sorted(Comparator.comparingInt(option -> integerValue(option.get("sortOrder"))))
                .limit(limit)
                .map(option -> new DmsMasterDataOptionResponse(
                        String.valueOf(option.get("value")),
                        String.valueOf(option.get("label")),
                        new LinkedHashMap<>(option)))
                .toList();
    }

    private int integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
