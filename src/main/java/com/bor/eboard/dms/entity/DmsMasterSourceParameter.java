package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import com.bor.eboard.dms.masterdata.DmsMasterParameterDataType;
import com.bor.eboard.dms.masterdata.DmsMasterParameterLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "dms_master_source_parameters")
public class DmsMasterSourceParameter extends BaseEntity {

    @Column(name = "master_source_id", nullable = false)
    private UUID masterSourceId;

    @Column(name = "parameter_name", nullable = false, length = 80)
    private String parameterName;

    @Column(name = "target_name", nullable = false, length = 120)
    private String targetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_location", nullable = false, length = 20)
    private DmsMasterParameterLocation parameterLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private DmsMasterParameterDataType dataType;

    @Column(name = "required", nullable = false)
    private Boolean required = Boolean.FALSE;

    @Column(name = "default_value", length = 2000)
    private String defaultValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public UUID getMasterSourceId() { return masterSourceId; }
    public void setMasterSourceId(UUID masterSourceId) { this.masterSourceId = masterSourceId; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public DmsMasterParameterLocation getParameterLocation() { return parameterLocation; }
    public void setParameterLocation(DmsMasterParameterLocation parameterLocation) { this.parameterLocation = parameterLocation; }
    public DmsMasterParameterDataType getDataType() { return dataType; }
    public void setDataType(DmsMasterParameterDataType dataType) { this.dataType = dataType; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
