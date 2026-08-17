package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import com.bor.eboard.dms.metadata.DmsMetadataConditionOperator;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.metadata.DmsMetadataDateConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dms_metadata_fields")
public class DmsMetadataField extends BaseEntity {

    @Column(name = "document_type_id", nullable = false)
    private UUID documentTypeId;

    @Column(name = "field_key", nullable = false, length = 80)
    private String fieldKey;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 30)
    private DmsMetadataControlType controlType;

    @Column(name = "placeholder", length = 250)
    private String placeholder;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @Column(name = "default_value", length = 4000)
    private String defaultValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "required", nullable = false)
    private Boolean required = Boolean.FALSE;

    @Column(name = "read_only", nullable = false)
    private Boolean readOnly = Boolean.FALSE;

    @Column(name = "hidden", nullable = false)
    private Boolean hidden = Boolean.FALSE;

    @Column(name = "searchable", nullable = false)
    private Boolean searchable = Boolean.TRUE;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "min_value", precision = 38, scale = 10)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 38, scale = 10)
    private BigDecimal maxValue;

    @Column(name = "regex_pattern", length = 1000)
    private String regexPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_constraint", nullable = false, length = 30)
    private DmsMetadataDateConstraint dateConstraint = DmsMetadataDateConstraint.NONE;

    @Column(name = "visibility_condition_field_key", length = 80)
    private String visibilityConditionFieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_condition_operator", length = 20)
    private DmsMetadataConditionOperator visibilityConditionOperator;

    @Column(name = "visibility_condition_value", length = 2000)
    private String visibilityConditionValue;

    @Column(name = "mandatory_condition_field_key", length = 80)
    private String mandatoryConditionFieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "mandatory_condition_operator", length = 20)
    private DmsMetadataConditionOperator mandatoryConditionOperator;

    @Column(name = "mandatory_condition_value", length = 2000)
    private String mandatoryConditionValue;

    @Column(name = "master_source_id")
    private UUID masterSourceId;

    @Column(name = "parent_field_id")
    private UUID parentFieldId;

    @Column(name = "source_parameter_name", length = 80)
    private String sourceParameterName;

    public UUID getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(UUID documentTypeId) { this.documentTypeId = documentTypeId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public DmsMetadataControlType getControlType() { return controlType; }
    public void setControlType(DmsMetadataControlType controlType) { this.controlType = controlType; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
    public Boolean getHidden() { return hidden; }
    public void setHidden(Boolean hidden) { this.hidden = hidden; }
    public Boolean getSearchable() { return searchable; }
    public void setSearchable(Boolean searchable) { this.searchable = searchable; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }
    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public String getRegexPattern() { return regexPattern; }
    public void setRegexPattern(String regexPattern) { this.regexPattern = regexPattern; }
    public DmsMetadataDateConstraint getDateConstraint() { return dateConstraint; }
    public void setDateConstraint(DmsMetadataDateConstraint dateConstraint) { this.dateConstraint = dateConstraint; }
    public String getVisibilityConditionFieldKey() { return visibilityConditionFieldKey; }
    public void setVisibilityConditionFieldKey(String value) { this.visibilityConditionFieldKey = value; }
    public DmsMetadataConditionOperator getVisibilityConditionOperator() { return visibilityConditionOperator; }
    public void setVisibilityConditionOperator(DmsMetadataConditionOperator value) { this.visibilityConditionOperator = value; }
    public String getVisibilityConditionValue() { return visibilityConditionValue; }
    public void setVisibilityConditionValue(String value) { this.visibilityConditionValue = value; }
    public String getMandatoryConditionFieldKey() { return mandatoryConditionFieldKey; }
    public void setMandatoryConditionFieldKey(String value) { this.mandatoryConditionFieldKey = value; }
    public DmsMetadataConditionOperator getMandatoryConditionOperator() { return mandatoryConditionOperator; }
    public void setMandatoryConditionOperator(DmsMetadataConditionOperator value) { this.mandatoryConditionOperator = value; }
    public String getMandatoryConditionValue() { return mandatoryConditionValue; }
    public void setMandatoryConditionValue(String value) { this.mandatoryConditionValue = value; }
    public UUID getMasterSourceId() { return masterSourceId; }
    public void setMasterSourceId(UUID masterSourceId) { this.masterSourceId = masterSourceId; }
    public UUID getParentFieldId() { return parentFieldId; }
    public void setParentFieldId(UUID parentFieldId) { this.parentFieldId = parentFieldId; }
    public String getSourceParameterName() { return sourceParameterName; }
    public void setSourceParameterName(String sourceParameterName) { this.sourceParameterName = sourceParameterName; }
}
