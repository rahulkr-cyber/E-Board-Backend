package com.bor.eboard.dms.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.bor.eboard.common.entity.BaseEntity;
import com.bor.eboard.dms.masterdata.DmsMasterHttpMethod;
import com.bor.eboard.dms.masterdata.DmsMasterSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "dms_master_sources")
public class DmsMasterSource extends BaseEntity {

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private DmsMasterSourceType sourceType;

    @Column(name = "value_field", nullable = false, length = 100)
    private String valueField;

    @Column(name = "label_field", nullable = false, length = 100)
    private String labelField;

    @Column(name = "response_path", length = 500)
    private String responsePath;

    @Column(name = "query_text", length = 10000)
    private String queryText;

    @Column(name = "procedure_name", length = 200)
    private String procedureName;

    @Column(name = "endpoint_url", length = 2000)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", length = 10)
    private DmsMasterHttpMethod httpMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_json", nullable = false)
    private String configurationJson = "{}";

    @Column(name = "cache_ttl_seconds", nullable = false)
    private Integer cacheTtlSeconds = 0;

    @Column(name = "max_results", nullable = false)
    private Integer maxResults = 500;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DmsMasterSourceType getSourceType() { return sourceType; }
    public void setSourceType(DmsMasterSourceType sourceType) { this.sourceType = sourceType; }
    public String getValueField() { return valueField; }
    public void setValueField(String valueField) { this.valueField = valueField; }
    public String getLabelField() { return labelField; }
    public void setLabelField(String labelField) { this.labelField = labelField; }
    public String getResponsePath() { return responsePath; }
    public void setResponsePath(String responsePath) { this.responsePath = responsePath; }
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public DmsMasterHttpMethod getHttpMethod() { return httpMethod; }
    public void setHttpMethod(DmsMasterHttpMethod httpMethod) { this.httpMethod = httpMethod; }
    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
    public Integer getCacheTtlSeconds() { return cacheTtlSeconds; }
    public void setCacheTtlSeconds(Integer cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
