package com.bor.eboard.dms.storage;

/**
 * Provider-specific health result used by the DMS administration API.
 */
public record StorageProviderHealth(boolean healthy, String message) {

    public static StorageProviderHealth healthy(String message) {
        return new StorageProviderHealth(true, message);
    }

    public static StorageProviderHealth unhealthy(String message) {
        return new StorageProviderHealth(false, message);
    }
}
