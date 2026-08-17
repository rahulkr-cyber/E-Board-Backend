package com.bor.eboard.dms.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dms_search_index")
public class DmsSearchIndex extends BaseEntity {

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "document_type_id", nullable = false)
    private UUID documentTypeId;

    @Column(name = "document_type_code", nullable = false, length = 50)
    private String documentTypeCode;

    @Column(name = "document_type_name", nullable = false, length = 150)
    private String documentTypeName;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "current_version_number", nullable = false)
    private Integer currentVersionNumber;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_by_name", length = 200)
    private String uploadedByName;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "document_updated_at")
    private LocalDateTime documentUpdatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
    private String metadataJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags_json", nullable = false, columnDefinition = "jsonb")
    private String tagsJson = "[]";

    @Column(name = "metadata_text", columnDefinition = "text")
    private String metadataText;

    @Column(name = "tags_text", columnDefinition = "text")
    private String tagsText;

    @Column(name = "latest_file_name", length = 500)
    private String latestFileName;

    @Column(name = "keywords_text", columnDefinition = "text")
    private String keywordsText;

    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public UUID getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(UUID documentTypeId) { this.documentTypeId = documentTypeId; }
    public String getDocumentTypeCode() { return documentTypeCode; }
    public void setDocumentTypeCode(String documentTypeCode) { this.documentTypeCode = documentTypeCode; }
    public String getDocumentTypeName() { return documentTypeName; }
    public void setDocumentTypeName(String documentTypeName) { this.documentTypeName = documentTypeName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentVersionNumber() { return currentVersionNumber; }
    public void setCurrentVersionNumber(Integer currentVersionNumber) { this.currentVersionNumber = currentVersionNumber; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }
    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDateTime getDocumentUpdatedAt() { return documentUpdatedAt; }
    public void setDocumentUpdatedAt(LocalDateTime documentUpdatedAt) { this.documentUpdatedAt = documentUpdatedAt; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public String getMetadataText() { return metadataText; }
    public void setMetadataText(String metadataText) { this.metadataText = metadataText; }
    public String getTagsText() { return tagsText; }
    public void setTagsText(String tagsText) { this.tagsText = tagsText; }
    public String getLatestFileName() { return latestFileName; }
    public void setLatestFileName(String latestFileName) { this.latestFileName = latestFileName; }
    public String getKeywordsText() { return keywordsText; }
    public void setKeywordsText(String keywordsText) { this.keywordsText = keywordsText; }
    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }
}
