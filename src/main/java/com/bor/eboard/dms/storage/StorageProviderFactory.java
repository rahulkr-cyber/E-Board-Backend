package com.bor.eboard.dms.storage;

import org.springframework.stereotype.Component;

/**
 * Selects the active provider without exposing provider-specific details to
 * future DMS document services.
 */
@Component
public class StorageProviderFactory {

    private final StorageConfigurationResolver configurationResolver;
    private final StorageProviderRegistry providerRegistry;

    public StorageProviderFactory(
            StorageConfigurationResolver configurationResolver,
            StorageProviderRegistry providerRegistry) {
        this.configurationResolver = configurationResolver;
        this.providerRegistry = providerRegistry;
    }

    public StorageProvider currentProvider() {
        String providerCode = configurationResolver.resolve().providerCode();
        return provider(providerCode);
    }

    public StorageProvider provider(String providerCode) {
        return providerRegistry.require(providerCode);
    }
}
