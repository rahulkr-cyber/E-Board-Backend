package com.bor.eboard.correspondence.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * correspondence_attachments table.
 * linked_entity_type: DIARY | FILE | LETTER | DISPATCH | CHARGE | TRANSFER.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "correspondence_attachments")
public class Attachment extends BaseEntity {

    @Column(name = "linked_entity_type", nullable = false, length = 50)
    private String linkedEntityType;

    @Column(name = "linked_entity_id", nullable = false)
    private UUID linkedEntityId;

    @Column(name = "document_type_id")
    private UUID documentTypeId;

    /** Optional link to an independent checklist item; storage remains shared. */
    @Column(name = "checklist_instance_item_id")
    private UUID checklistInstanceItemId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "checksum", length = 256)
    private String checksum;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
