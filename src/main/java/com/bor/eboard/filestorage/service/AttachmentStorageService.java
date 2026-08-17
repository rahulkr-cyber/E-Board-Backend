package com.bor.eboard.filestorage.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Abstracted document storage (02_ARCHITECTURE.md section 21):
 * local disk today, NAS/object storage later without touching callers.
 * Files are stored with generated names, never original names, and a
 * SHA-256 checksum is recorded for integrity verification.
 */
public interface AttachmentStorageService {

    record StoredAttachment(
            UUID id,
            String originalFileName,
            String fileExtension,
            String mimeType,
            long fileSize,
            String checksum,
            java.time.LocalDateTime uploadedAt) {
    }

    record DownloadableAttachment(
            Resource resource,
            String originalFileName,
            String mimeType) {
    }

    StoredAttachment store(String linkedEntityType, UUID linkedEntityId,
                           UUID documentTypeId, MultipartFile file);

    /** Stores in the same attachment table while linking the file to a checklist item. */
    StoredAttachment store(String linkedEntityType, UUID linkedEntityId,
                           UUID documentTypeId, UUID checklistInstanceItemId,
                           MultipartFile file);

    List<StoredAttachment> listFor(String linkedEntityType, UUID linkedEntityId);

    DownloadableAttachment download(UUID attachmentId);

    /** Marks an attachment inactive (records are never physically removed). */
    void deactivate(UUID attachmentId);

    /** Re-links all diary attachments to the generated inward letter. */
    void linkDiaryAttachmentsToLetter(UUID diaryId, UUID letterId);
}
