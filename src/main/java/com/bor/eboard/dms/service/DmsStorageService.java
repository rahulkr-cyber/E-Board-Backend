package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.StorageConfigurationResponse;
import com.bor.eboard.dms.dto.StorageConfigurationUpdateRequest;
import com.bor.eboard.dms.dto.StorageHealthResponse;
import com.bor.eboard.dms.dto.StorageProviderResponse;
import com.bor.eboard.dms.storage.StorageProvider;

import java.util.List;

/**
 * DMS storage administration and provider access boundary.
 */
public interface DmsStorageService {

    StorageConfigurationResponse getConfiguration();

    StorageHealthResponse checkHealth();

    StorageConfigurationResponse updateConfiguration(StorageConfigurationUpdateRequest request);

    List<StorageProviderResponse> listProviders();

    StorageProvider currentProvider();
}
