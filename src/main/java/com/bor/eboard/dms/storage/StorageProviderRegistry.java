package com.bor.eboard.dms.storage;

import com.bor.eboard.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry of DMS-only storage implementations discovered from Spring.
 */
@Component
public class StorageProviderRegistry {

    private final Map<String, StorageProvider> providers;

    public StorageProviderRegistry(List<StorageProvider> storageProviders) {
        Map<String, StorageProvider> registered = new LinkedHashMap<>();
        for (StorageProvider provider : storageProviders) {
            String code = normalize(provider.providerCode());
            StorageProvider previous = registered.putIfAbsent(code, provider);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate DMS storage provider registration: " + code);
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public StorageProvider require(String providerCode) {
        return find(providerCode)
                .orElseThrow(() -> new BusinessException(
                        "DMS storage provider implementation is unavailable: "
                                + normalize(providerCode)));
    }

    public Optional<StorageProvider> find(String providerCode) {
        return Optional.ofNullable(providers.get(normalize(providerCode)));
    }

    public Set<String> registeredProviderCodes() {
        return providers.keySet();
    }

    private String normalize(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new BusinessException("DMS storage provider code is missing");
        }
        return providerCode.trim().toLowerCase(Locale.ROOT);
    }
}
