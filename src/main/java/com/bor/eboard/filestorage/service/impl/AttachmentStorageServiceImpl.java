package com.bor.eboard.filestorage.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.FileStorageException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.entity.Attachment;
import com.bor.eboard.correspondence.repository.AttachmentRepository;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Local-disk storage provider. Layout: {base}/{yyyy}/{MM}/{uuid}.{ext}.
 * Enforces an allow-list of extensions and a configurable size limit
 * (09_SECURITY.md upload rules).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentStorageServiceImpl implements AttachmentStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "tif", "tiff",
            "doc", "docx", "xls", "xlsx", "odt", "ods", "txt");

    private final AttachmentRepository attachmentRepository;
    private final AuditService auditService;

    @Value("${file-storage.base-path}")
    private String basePath;

    @Value("${spring.servlet.multipart.max-file-size:20MB}")
    private String maxFileSize;

    @Override
    @Transactional
    public StoredAttachment store(String linkedEntityType, UUID linkedEntityId,
                                  UUID documentTypeId, MultipartFile file) {
        return store(linkedEntityType, linkedEntityId, documentTypeId, null, file);
    }

    @Override
    @Transactional
    public StoredAttachment store(String linkedEntityType, UUID linkedEntityId,
                                  UUID documentTypeId, UUID checklistInstanceItemId,
                                  MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Uploaded file is empty");
        }
        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(
                    "File type not allowed. Permitted types: " + String.join(", ",
                            ALLOWED_EXTENSIONS.stream().sorted().toList()));
        }

        UUID uploaderId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        LocalDate today = LocalDate.now();
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path directory = Paths.get(basePath,
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()));
        Path target = directory.resolve(storedFileName);

        String checksum;
        try {
            Files.createDirectories(directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            checksum = HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + originalFileName, e);
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("SHA-256 unavailable", e);
        }

        Attachment attachment = new Attachment();
        attachment.setLinkedEntityType(linkedEntityType);
        attachment.setLinkedEntityId(linkedEntityId);
        attachment.setDocumentTypeId(documentTypeId);
        attachment.setChecklistInstanceItemId(checklistInstanceItemId);
        attachment.setOriginalFileName(originalFileName);
        attachment.setStoredFileName(storedFileName);
        attachment.setFileExtension(extension);
        attachment.setMimeType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(target.toString());
        attachment.setChecksum(checksum);
        attachment.setUploadedBy(uploaderId);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setActive(Boolean.TRUE);
        attachment = attachmentRepository.save(attachment);

        auditService.record("FILESTORAGE", "ATTACHMENT", attachment.getId(),
                "UPLOAD", null,
                "entity=" + linkedEntityType + ":" + linkedEntityId
                        + ", file=" + originalFileName + ", size=" + file.getSize());

        return toStored(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredAttachment> listFor(String linkedEntityType, UUID linkedEntityId) {
        return attachmentRepository
                .findByLinkedEntityTypeAndLinkedEntityIdAndActiveTrueAndDeletedFalseOrderByUploadedAtAsc(
                        linkedEntityType, linkedEntityId)
                .stream()
                .map(this::toStored)
                .toList();
    }

    
    
    @Override
    @Transactional(readOnly = true)
    public DownloadableAttachment download(UUID attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        try {
            Path path = Paths.get(attachment.getStoragePath());
            UrlResource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException(
                        "Stored file is missing or unreadable: " + attachmentId);
            }
            return new DownloadableAttachment(resource,
                    attachment.getOriginalFileName(),
                    attachment.getMimeType() != null
                            ? attachment.getMimeType()
                            : "application/octet-stream");
        } catch (IOException e) {
            throw new FileStorageException("Failed to read stored file", e);
        }
    }

    @Override
    @Transactional
    public void deactivate(UUID attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        attachment.setActive(Boolean.FALSE);
        attachmentRepository.save(attachment);
        auditService.record("FILESTORAGE", "ATTACHMENT", attachmentId,
                "DEACTIVATE", "active=true", "active=false");
    }

    private Attachment findAttachment(UUID id) {
        return attachmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", id));
    }

    private StoredAttachment toStored(Attachment attachment) {
        return new StoredAttachment(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getFileExtension(),
                attachment.getMimeType(),
                attachment.getFileSize() != null ? attachment.getFileSize() : 0L,
                attachment.getChecksum(),
                attachment.getUploadedAt());
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("File name is missing");
        }
        // Strip any path components and control characters.
        String cleaned = Paths.get(name).getFileName().toString()
                .replaceAll("[\\r\\n\\t\\\\/:*?\"<>|]", "_");
        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(cleaned.length() - 255);
        }
        return cleaned;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new ValidationException("File must have an extension");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    
    
    @Override
    @Transactional
    public void linkDiaryAttachmentsToLetter(UUID diaryId, UUID letterId) {

        List<Attachment> attachments = attachmentRepository
            .findByLinkedEntityTypeAndLinkedEntityIdAndActiveTrueAndDeletedFalseOrderByUploadedAtAsc(
                "DIARY", diaryId
            );

        for (Attachment att : attachments) {
            att.setLinkedEntityType("LETTER");
            att.setLinkedEntityId(letterId);
        }

        attachmentRepository.saveAll(attachments);
    }

}
