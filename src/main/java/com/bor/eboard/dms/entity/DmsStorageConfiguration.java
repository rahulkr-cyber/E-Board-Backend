package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider configuration owned exclusively by the DMS module.
 */
@Entity
@Table(name = "dms_storage_configuration")
public class DmsStorageConfiguration extends BaseEntity {

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "base_path", length = 1000)
    private String basePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configuration = new HashMap<>();

    @Column(name = "primary_provider", nullable = false)
    private Boolean primaryProvider = Boolean.FALSE;

    @Column(name = "health_check_enabled", nullable = false)
    private Boolean healthCheckEnabled = Boolean.TRUE;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Boolean getPrimaryProvider() {
        return primaryProvider;
    }

    public void setPrimaryProvider(Boolean primaryProvider) {
        this.primaryProvider = primaryProvider;
    }

    public Boolean getHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(Boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
