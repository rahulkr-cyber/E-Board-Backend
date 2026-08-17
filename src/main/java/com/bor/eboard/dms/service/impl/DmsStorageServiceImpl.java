package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.StorageConfigurationResponse;
import com.bor.eboard.dms.dto.StorageConfigurationUpdateRequest;
import com.bor.eboard.dms.dto.StorageHealthResponse;
import com.bor.eboard.dms.dto.StorageProviderResponse;
import com.bor.eboard.dms.entity.DmsStorageConfiguration;
import com.bor.eboard.dms.repository.DmsStorageConfigurationRepository;
import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.dms.service.DmsStorageService;
import com.bor.eboard.dms.storage.StorageConfigurationResolver;
import com.bor.eboard.dms.storage.StorageProvider;
import com.bor.eboard.dms.storage.StorageProviderFactory;
import com.bor.eboard.dms.storage.StorageProviderHealth;
import com.bor.eboard.dms.storage.StorageProviderRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DmsStorageServiceImpl implements DmsStorageService {

    private final StorageConfigurationResolver configurationResolver;
    private final StorageProviderRegistry providerRegistry;
    private final StorageProviderFactory providerFactory;
    private final DmsStorageConfigurationRepository configurationRepository;
    private final DmsAuditTrailService auditTrailService;

    public DmsStorageServiceImpl(
            StorageConfigurationResolver configurationResolver,
            StorageProviderRegistry providerRegistry,
            StorageProviderFactory providerFactory,
            DmsStorageConfigurationRepository configurationRepository,
            DmsAuditTrailService auditTrailService) {
        this.configurationResolver = configurationResolver;
        this.providerRegistry = providerRegistry;
        this.providerFactory = providerFactory;
        this.configurationRepository = configurationRepository;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public StorageConfigurationResponse getConfiguration() {
        return toResponse(configurationResolver.resolve());
    }

    @Override
    @Transactional(readOnly = true)
    public StorageHealthResponse checkHealth() {
        StorageConfigurationResolver.ResolvedStorageConfiguration configuration =
                configurationResolver.resolve();

        StorageProviderHealth health;
        if (!configuration.healthCheckEnabled()) {
            health = StorageProviderHealth.healthy(
                    "DMS storage health checks are disabled for this provider");
        } else {
            health = providerRegistry.find(configuration.providerCode())
                    .map(StorageProvider::health)
                    .orElseGet(() -> StorageProviderHealth.unhealthy(
                            "DMS storage provider implementation is unavailable"));
        }

        return new StorageHealthResponse(
                configuration.providerCode(),
                health.healthy(),
                health.message(),
                LocalDateTime.now());
    }

    @Override
    @Transactional
    public StorageConfigurationResponse updateConfiguration(
            StorageConfigurationUpdateRequest request) {
        String providerCode = normalizeProviderCode(request.providerCode());
        StorageProvider provider = providerRegistry.require(providerCode);
        String displayName = request.displayName().trim();
        String basePath = normalizeOptional(request.basePath());
        Map<String, Object> providerConfiguration = safeConfiguration(request.configuration());

        if (StorageConfigurationResolver.LOCAL_PROVIDER.equals(providerCode)
                && basePath == null) {
            throw new ValidationException("DMS local storage base path is required");
        }

        StorageConfigurationResolver.ResolvedStorageConfiguration previous =
                configurationResolver.resolve();

        for (DmsStorageConfiguration currentPrimary :
                configurationRepository.findByPrimaryProviderTrueAndDeletedFalse()) {
            currentPrimary.setPrimaryProvider(Boolean.FALSE);
            configurationRepository.save(currentPrimary);
        }

        DmsStorageConfiguration target = configurationRepository
                .findByProviderCodeIgnoreCaseAndDeletedFalse(providerCode)
                .orElseGet(DmsStorageConfiguration::new);
        target.setProviderCode(providerCode);
        target.setDisplayName(displayName);
        target.setBasePath(basePath);
        target.setConfiguration(providerConfiguration);
        target.setActive(Boolean.TRUE);
        target.setPrimaryProvider(Boolean.TRUE);
        target.setHealthCheckEnabled(request.healthCheckEnabled() == null
                ? Boolean.TRUE
                : request.healthCheckEnabled());
        target.setDeleted(Boolean.FALSE);
        target = configurationRepository.saveAndFlush(target);

        StorageProviderHealth validationHealth = provider.health();
        if (!validationHealth.healthy()) {
            throw new ValidationException(
                    "DMS storage configuration failed health validation: "
                            + validationHealth.message());
        }

        String oldValue = auditValue(previous.providerCode(), previous.displayName(),
                previous.effectiveBasePath(), previous.healthCheckEnabled());
        StorageConfigurationResolver.ResolvedStorageConfiguration updated =
                configurationResolver.resolve();
        auditTrailService.record(
                "STORAGE_CONFIGURATION",
                target.getId(),
                "UPDATE",
                oldValue,
                auditValue(updated.providerCode(), updated.displayName(),
                        updated.effectiveBasePath(), updated.healthCheckEnabled()));
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageProviderResponse> listProviders() {
        StorageConfigurationResolver.ResolvedStorageConfiguration current =
                configurationResolver.resolve();
        return providerRegistry.registeredProviderCodes().stream()
                .sorted()
                .map(code -> providerResponse(code, current.providerCode()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StorageProvider currentProvider() {
        return providerFactory.currentProvider();
    }

    private StorageProviderResponse providerResponse(String code, String currentProviderCode) {
        DmsStorageConfiguration configuration = configurationRepository
                .findByProviderCodeIgnoreCaseAndDeletedFalse(code)
                .orElse(null);
        boolean configured = configuration != null;
        boolean active = configured && Boolean.TRUE.equals(configuration.getActive());
        boolean primary = code.equalsIgnoreCase(currentProviderCode);

        if (!configured && !primary) {
            return new StorageProviderResponse(
                    code, false, false, false, false, "Provider is not configured");
        }

        StorageProviderHealth health = providerRegistry.require(code).health();
        return new StorageProviderResponse(
                code,
                configured || primary,
                active || primary,
                primary,
                health.healthy(),
                health.message());
    }

    private StorageConfigurationResponse toResponse(
            StorageConfigurationResolver.ResolvedStorageConfiguration configuration) {
        return new StorageConfigurationResponse(
                configuration.id(),
                configuration.providerCode(),
                configuration.displayName(),
                configuration.effectiveBasePath(),
                configuration.active(),
                configuration.primaryProvider(),
                configuration.healthCheckEnabled(),
                configuration.persisted());
    }

    private String normalizeProviderCode(String providerCode) {
        return providerCode.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> safeConfiguration(Map<String, Object> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return Collections.emptyMap();
        }
        return new HashMap<>(configuration);
    }

    private String auditValue(
            String providerCode,
            String displayName,
            String basePath,
            boolean healthCheckEnabled) {
        return "provider=" + providerCode
                + ", displayName=" + displayName
                + ", basePath=" + basePath
                + ", healthCheckEnabled=" + healthCheckEnabled;
    }
}
