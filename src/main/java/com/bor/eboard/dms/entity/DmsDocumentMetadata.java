package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "dms_document_metadata")
public class DmsDocumentMetadata extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "metadata_field_id", nullable = false)
    private UUID metadataFieldId;

    @Column(name = "field_key", nullable = false, length = 80)
    private String fieldKey;

    @Column(name = "field_label", nullable = false, length = 150)
    private String fieldLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", nullable = false, columnDefinition = "jsonb")
    private String valueJson;

    @Column(name = "searchable", nullable = false)
    private Boolean searchable = Boolean.TRUE;

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getMetadataFieldId() { return metadataFieldId; }
    public void setMetadataFieldId(UUID metadataFieldId) { this.metadataFieldId = metadataFieldId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }
    public Boolean getSearchable() { return searchable; }
    public void setSearchable(Boolean searchable) { this.searchable = searchable; }
}
