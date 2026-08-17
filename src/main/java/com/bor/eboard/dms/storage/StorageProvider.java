package com.bor.eboard.dms.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provider contract for storage owned exclusively by the DMS module.
 */
public interface StorageProvider {

    String providerCode();

    String store(InputStream inputStream,
                 long contentLength,
                 String originalFilename,
                 String contentType) throws IOException;

    Resource load(String storageKey);

    boolean exists(String storageKey);

    void delete(String storageKey) throws IOException;

    StorageProviderHealth health();
}
