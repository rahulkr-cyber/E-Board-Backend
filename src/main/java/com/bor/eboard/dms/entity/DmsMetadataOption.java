package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "dms_metadata_options")
public class DmsMetadataOption extends BaseEntity {

    @Column(name = "metadata_field_id", nullable = false)
    private UUID metadataFieldId;

    @Column(name = "option_value", nullable = false, length = 500)
    private String value;

    @Column(name = "option_label", nullable = false, length = 500)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public UUID getMetadataFieldId() { return metadataFieldId; }
    public void setMetadataFieldId(UUID metadataFieldId) { this.metadataFieldId = metadataFieldId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
