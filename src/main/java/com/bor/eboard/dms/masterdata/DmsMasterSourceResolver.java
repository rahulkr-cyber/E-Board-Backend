package com.bor.eboard.dms.masterdata;

import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DmsMasterSourceResolver {
    Set<DmsMasterSourceType> supportedTypes();

    List<DmsMasterDataOptionResponse> resolve(
            DmsMasterSource source,
            List<DmsMasterSourceParameter> parameterDefinitions,
            Map<String, Object> parameters);
}
