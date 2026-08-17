package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DmsMasterSourceResolverRegistry {

    private final Map<DmsMasterSourceType, DmsMasterSourceResolver> resolvers =
            new EnumMap<>(DmsMasterSourceType.class);

    public DmsMasterSourceResolverRegistry(List<DmsMasterSourceResolver> resolverList) {
        for (DmsMasterSourceResolver resolver : resolverList) {
            for (DmsMasterSourceType type : resolver.supportedTypes()) {
                DmsMasterSourceResolver previous = resolvers.put(type, resolver);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate DMS master source resolver for " + type);
                }
            }
        }
    }

    public DmsMasterSourceResolver get(DmsMasterSourceType type) {
        DmsMasterSourceResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new BusinessException("No DMS master source resolver is registered for " + type);
        }
        return resolver;
    }
}
