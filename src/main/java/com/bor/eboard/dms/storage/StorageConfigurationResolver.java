package com.bor.eboard.dms.storage;

import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.dms.config.DmsProperties;
import com.bor.eboard.dms.entity.DmsStorageConfiguration;
import com.bor.eboard.dms.repository.DmsStorageConfigurationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves the effective DMS provider configuration. The database primary
 * provider is preferred; application properties are used as a safe fallback.
 */
@Component
public class StorageConfigurationResolver {

    public static final String LOCAL_PROVIDER = "local";

    private final DmsStorageConfigurationRepository configurationRepository;
    private final DmsProperties properties;

    public StorageConfigurationResolver(
            DmsStorageConfigurationRepository configurationRepository,
            DmsProperties properties) {
        this.configurationRepository = configurationRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public ResolvedStorageConfiguration resolve() {
        return configurationRepository
                .findFirstByPrimaryProviderTrueAndActiveTrueAndDeletedFalse()
                .map(this::toResolved)
                .orElseGet(this::resolvePropertyFallback);
    }

    @Transactional(readOnly = true)
    public ResolvedStorageConfiguration resolve(String providerCode) {
        String normalized = normalizeProviderCode(providerCode);
        return configurationRepository
                .findByProviderCodeIgnoreCaseAndDeletedFalse(normalized)
                .map(this::toResolved)
                .orElseGet(() -> resolveProviderPropertyFallback(normalized));
    }

    private ResolvedStorageConfiguration toResolved(DmsStorageConfiguration configuration) {
        String providerCode = normalizeProviderCode(configuration.getProviderCode());
        String effectiveBasePath = resolveBasePath(providerCode, configuration.getBasePath());
        return new ResolvedStorageConfiguration(
                configuration.getId(),
                providerCode,
                configuration.getDisplayName(),
                effectiveBasePath,
                Boolean.TRUE.equals(configuration.getActive()),
                Boolean.TRUE.equals(configuration.getPrimaryProvider()),
                Boolean.TRUE.equals(configuration.getHealthCheckEnabled()),
                safeConfiguration(configuration.getConfiguration()),
                true);
    }

    private ResolvedStorageConfiguration resolvePropertyFallback() {
        String providerCode = normalizeProviderCode(properties.getStorage().getProvider());
        DmsStorageConfiguration storedConfiguration = configurationRepository
                .findByProviderCodeIgnoreCaseAndActiveTrueAndDeletedFalse(providerCode)
                .orElse(null);
        if (storedConfiguration != null) {
            return toResolved(storedConfiguration);
        }

        return new ResolvedStorageConfiguration(
                null,
                providerCode,
                providerCode.toUpperCase(Locale.ROOT),
                resolveBasePath(providerCode, null),
                true,
                true,
                true,
                Collections.emptyMap(),
                false);
    }

    private ResolvedStorageConfiguration resolveProviderPropertyFallback(String providerCode) {
        String configuredProvider = normalizeProviderCode(properties.getStorage().getProvider());
        if (!configuredProvider.equals(providerCode)) {
            throw new BusinessException("DMS storage provider configuration was not found: " + providerCode);
        }
        return new ResolvedStorageConfiguration(
                null,
                providerCode,
                providerCode.toUpperCase(Locale.ROOT),
                resolveBasePath(providerCode, null),
                true,
                true,
                true,
                Collections.emptyMap(),
                false);
    }

    private Map<String, Object> safeConfiguration(Map<String, Object> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new java.util.HashMap<>(configuration));
    }

    private String resolveBasePath(String providerCode, String configuredBasePath) {
        if (configuredBasePath != null && !configuredBasePath.isBlank()) {
            return configuredBasePath.trim();
        }
        if (LOCAL_PROVIDER.equals(providerCode)) {
            String propertyBasePath = properties.getStorage().getLocalBasePath();
            if (propertyBasePath == null || propertyBasePath.isBlank()) {
                throw new BusinessException("DMS local storage base path is not configured");
            }
            return propertyBasePath.trim();
        }
        return null;
    }

    private String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new BusinessException("DMS storage provider is not configured");
        }
        return providerCode.trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedStorageConfiguration(
            UUID id,
            String providerCode,
            String displayName,
            String effectiveBasePath,
            boolean active,
            boolean primaryProvider,
            boolean healthCheckEnabled,
            Map<String, Object> providerConfiguration,
            boolean persisted) {
    }
}
