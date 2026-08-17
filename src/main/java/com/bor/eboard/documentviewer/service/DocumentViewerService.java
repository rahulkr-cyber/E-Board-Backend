package com.bor.eboard.documentviewer.service;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.entity.Attachment;
import com.bor.eboard.correspondence.repository.AttachmentRepository;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsDocumentResponse;
import com.bor.eboard.dms.dto.DmsDocumentVersionResponse;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.service.DmsDocumentService;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.repository.DepartmentRepository;
import com.bor.eboard.identity.repository.SectionRepository;
import com.bor.eboard.identity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentViewerService {

    public static final String DOWNLOAD_PERMISSION = "DOCUMENT_DOWNLOAD";
    public static final String PRINT_PERMISSION = "DOCUMENT_PRINT";
    public static final String SHARE_PERMISSION = "DOCUMENT_SHARE";

    private final AttachmentAccessService attachmentAccessService;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final DmsDocumentService dmsDocumentService;
    private final DocumentShareTokenService tokenService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    @Value("${document-viewer.watermark.enabled:true}")
    private boolean watermarkEnabled;

    @Value("${document-viewer.watermark.organization:Government of Uttar Pradesh}")
    private String watermarkOrganization;

    @Value("${document-viewer.watermark.office:Board of Revenue}")
    private String watermarkOffice;

    @Value("${document-viewer.watermark.include-ip:true}")
    private boolean watermarkIncludeIp;

    @Value("${document-viewer.sharing.allowed-expiry-minutes:30,60,1440}")
    private String allowedExpiryMinutes;

    @Transactional(readOnly = true)
    public Metadata metadata(Source source, UUID id, Integer version, HttpServletRequest request) {
        try {
            return source == Source.ATTACHMENT
                    ? attachmentMetadata(id, request)
                    : dmsMetadata(id, version, request);
        } catch (ForbiddenException ex) {
            auditService.recordFailure("DOCUMENT_VIEWER", source.name(), id,
                    "FAILED_ACCESS", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Content view(Source source, UUID id, Integer version) {
        try {
            if (source == Source.ATTACHMENT) {
                Attachment attachment = attachmentAccessService.requireView(id);
                AttachmentStorageService.DownloadableAttachment content = attachmentStorageService.download(id);
                auditService.record("DOCUMENT_VIEWER", "ATTACHMENT", id,
                        "VIEW", null, ownerReference(attachment));
                return new Content(content.resource(), content.originalFileName(),
                        content.mimeType(), size(attachment.getFileSize()));
            }
            requireDmsViewPermission();
            DmsDocumentService.DownloadableDocument content = dmsDocumentService.view(id, version);
            return new Content(content.resource(), content.originalFileName(),
                    content.mimeType(), content.fileSize());
        } catch (ForbiddenException ex) {
            auditService.recordFailure("DOCUMENT_VIEWER", source.name(), id,
                    "FAILED_ACCESS", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Content download(Source source, UUID id, Integer version) {
        requirePermission(DOWNLOAD_PERMISSION, source, id);
        try {
            if (source == Source.ATTACHMENT) {
                Attachment attachment = attachmentAccessService.requireView(id);
                AttachmentStorageService.DownloadableAttachment content = attachmentStorageService.download(id);
                auditService.record("DOCUMENT_VIEWER", "ATTACHMENT", id,
                        "DOWNLOAD", null, ownerReference(attachment));
                return new Content(content.resource(), content.originalFileName(),
                        content.mimeType(), size(attachment.getFileSize()));
            }
            requireAnyPermission(DmsPermissions.DOWNLOAD, DmsPermissions.ADMIN);
            DmsDocumentService.DownloadableDocument content = dmsDocumentService.download(id, version);
            return new Content(content.resource(), content.originalFileName(),
                    content.mimeType(), content.fileSize());
        } catch (ForbiddenException ex) {
            auditService.recordFailure("DOCUMENT_VIEWER", source.name(), id,
                    "FAILED_ACCESS", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public void authorizePrint(Source source, UUID id, Integer version) {
        requirePermission(PRINT_PERMISSION, source, id);
        metadata(source, id, version, currentRequest());
        auditService.record("DOCUMENT_VIEWER", source.name(), id,
                "PRINT", null, version == null ? null : "version=" + version);
    }

    @Transactional(readOnly = true)
    public ShareResponse share(Source source, UUID id, Integer version, Integer expiryMinutes) {
        requirePermission(SHARE_PERMISSION, source, id);
        int expiry = validateExpiry(expiryMinutes);
        Metadata metadata = metadata(source, id, version, currentRequest());
        if (!metadata.canShare()) {
            String message = "Share access is not available for this document";
            auditService.recordFailure("DOCUMENT_VIEWER", source.name(), id,
                    "FAILED_ACCESS", message);
            throw new ForbiddenException(message);
        }
        Instant expiresAt = Instant.now().plusSeconds(expiry * 60L);
        String issuedBy = currentUser().map(User::getFullName).orElse("Authenticated user");
        String token = tokenService.create(new DocumentShareTokenService.ShareGrant(
                source.name(), id, version, metadata.fileName(), metadata.mimeType(),
                metadata.fileSize(), issuedBy, expiresAt));
        auditService.record("DOCUMENT_VIEWER", source.name(), id,
                "SHARE", null, "expiryMinutes=" + expiry + (version == null ? "" : ",version=" + version));
        return new ShareResponse(token, expiresAt);
    }

    public SharedMetadata sharedMetadata(String token, HttpServletRequest request) {
        try {
            DocumentShareTokenService.ShareGrant grant = tokenService.parse(token);
            return new SharedMetadata(
                    grant.source(), grant.documentId(), grant.version(), grant.fileName(),
                    grant.mimeType(), grant.fileSize(), viewerType(grant.fileName(), grant.mimeType()),
                    List.of(), watermark("Secure shared link", request), grant.expiresAt());
        } catch (ForbiddenException ex) {
            auditService.recordFailure("DOCUMENT_VIEWER", "SHARED_LINK", null,
                    "FAILED_ACCESS", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Content sharedContent(String token) {
        DocumentShareTokenService.ShareGrant grant;
        try {
            grant = tokenService.parse(token);
        } catch (ForbiddenException ex) {
            auditService.recordFailure("DOCUMENT_VIEWER", "SHARED_LINK", null,
                    "FAILED_ACCESS", ex.getMessage());
            throw ex;
        }
        Source source = Source.from(grant.source());
        if (source == Source.ATTACHMENT) {
            Attachment attachment = attachmentRepository.findByIdAndDeletedFalse(grant.documentId())
                    .filter(value -> Boolean.TRUE.equals(value.getActive()))
                    .orElseThrow(() -> new ResourceNotFoundException("Attachment", grant.documentId()));
            AttachmentStorageService.DownloadableAttachment content =
                    attachmentStorageService.download(grant.documentId());
            auditService.record("DOCUMENT_VIEWER", "ATTACHMENT", grant.documentId(),
                    "SHARED_VIEW", null, ownerReference(attachment));
            return new Content(content.resource(), content.originalFileName(),
                    content.mimeType(), size(attachment.getFileSize()));
        }
        DmsDocumentService.DownloadableDocument content =
                dmsDocumentService.sharedView(grant.documentId(), grant.version());
        return new Content(content.resource(), content.originalFileName(),
                content.mimeType(), content.fileSize());
    }

    private Metadata attachmentMetadata(UUID id, HttpServletRequest request) {
        Attachment attachment = attachmentAccessService.requireView(id);
        return new Metadata(Source.ATTACHMENT.name(), id, null,
                attachment.getOriginalFileName(), safeMime(attachment.getMimeType()),
                size(attachment.getFileSize()), viewerType(attachment.getOriginalFileName(), attachment.getMimeType()),
                SecurityUtils.hasAuthority(DOWNLOAD_PERMISSION),
                SecurityUtils.hasAuthority(PRINT_PERMISSION),
                SecurityUtils.hasAuthority(SHARE_PERMISSION),
                watermark(currentUser().map(User::getFullName).orElse("Authenticated user"), request),
                allowedExpiries(), List.of());
    }

    private Metadata dmsMetadata(UUID id, Integer version, HttpServletRequest request) {
        requireDmsViewPermission();
        DmsDocumentResponse document = dmsDocumentService.findById(id);
        DmsDocumentVersionResponse selected = version == null
                ? document.latestVersion()
                : dmsDocumentService.findVersions(id).stream()
                        .filter(item -> version.equals(item.versionNumber()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("DMS document version", version));
        Set<DmsDocumentAccessLevel> access = document.access().effectiveAccessLevels();
        boolean objectDownload = access.contains(DmsDocumentAccessLevel.DOWNLOAD);
        return new Metadata(Source.DMS.name(), id, selected.versionNumber(),
                selected.originalFileName(), safeMime(selected.mimeType()), size(selected.fileSize()),
                viewerType(selected.originalFileName(), selected.mimeType()),
                SecurityUtils.hasAuthority(DOWNLOAD_PERMISSION)
                        && hasAnyPermission(DmsPermissions.DOWNLOAD, DmsPermissions.ADMIN)
                        && objectDownload,
                SecurityUtils.hasAuthority(PRINT_PERMISSION),
                SecurityUtils.hasAuthority(SHARE_PERMISSION)
                        && hasAnyPermission(DmsPermissions.SHARE, DmsPermissions.ADMIN)
                        && access.contains(DmsDocumentAccessLevel.SHARE),
                watermark(currentUser().map(User::getFullName).orElse("Authenticated user"), request),
                allowedExpiries(), List.of());
    }

    private Watermark watermark(String viewedBy, HttpServletRequest request) {
        if (!watermarkEnabled) {
            return new Watermark(false, List.of());
        }
        User user = currentUser().orElse(null);
        String department = user == null || user.getDepartmentId() == null ? null
                : departmentRepository.findByIdAndDeletedFalse(user.getDepartmentId())
                        .map(value -> value.getName()).orElse(null);
        String section = user == null || user.getSectionId() == null ? null
                : sectionRepository.findByIdAndDeletedFalse(user.getSectionId())
                        .map(value -> value.getName()).orElse(null);
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add(watermarkOrganization);
        lines.add(watermarkOffice);
        lines.add("Viewed By: " + viewedBy);
        if (department != null) {
            lines.add(department + (section == null ? "" : " / " + section));
        }
        lines.add("Date & Time: " + LocalDateTime.now());
        if (watermarkIncludeIp && request != null) {
            lines.add("IP Address: " + clientIp(request));
        }
        return new Watermark(true, List.copyOf(lines));
    }

    private java.util.Optional<User> currentUser() {
        return SecurityUtils.getCurrentUserId().flatMap(userRepository::findByIdAndDeletedFalse);
    }

    private HttpServletRequest currentRequest() {
        org.springframework.web.context.request.ServletRequestAttributes attributes =
                (org.springframework.web.context.request.ServletRequestAttributes)
                        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    private List<Integer> allowedExpiries() {
        return Arrays.stream(allowedExpiryMinutes.split(","))
                .map(String::trim).filter(value -> !value.isBlank())
                .map(Integer::valueOf).distinct().sorted().toList();
    }

    private int validateExpiry(Integer requested) {
        List<Integer> allowed = allowedExpiries();
        int value = requested == null ? allowed.get(0) : requested;
        if (!allowed.contains(value)) {
            throw new ValidationException("Unsupported secure-link expiry");
        }
        return value;
    }

    private void requirePermission(String permission, Source source, UUID id) {
        if (!SecurityUtils.hasAuthority(permission)) {
            String message = "You do not have " + permission + " permission";
            auditService.recordFailure("DOCUMENT_VIEWER", source.name(), id,
                    "FAILED_ACCESS", message);
            throw new ForbiddenException(message);
        }
    }

    private void requireDmsViewPermission() {
        requireAnyPermission(
                DmsPermissions.VIEW, DmsPermissions.CREATE, DmsPermissions.UPLOAD,
                DmsPermissions.UPDATE, DmsPermissions.DOWNLOAD, DmsPermissions.SHARE,
                DmsPermissions.ADMIN);
    }

    private void requireAnyPermission(String... permissions) {
        if (!hasAnyPermission(permissions)) {
            throw new ForbiddenException("You do not have access to the DMS module");
        }
    }

    private boolean hasAnyPermission(String... permissions) {
        return Arrays.stream(permissions).anyMatch(SecurityUtils::hasAuthority);
    }

    private String ownerReference(Attachment attachment) {
        return "entity=" + attachment.getLinkedEntityType() + ":" + attachment.getLinkedEntityId();
    }

    private long size(Long value) {
        return value == null ? 0L : value;
    }

    private String safeMime(String value) {
        return value == null || value.isBlank() ? "application/octet-stream" : value;
    }

    private String viewerType(String fileName, String mimeType) {
        String mime = safeMime(mimeType).toLowerCase(Locale.ROOT);
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (mime.equals("application/pdf") || name.endsWith(".pdf")) {
            return "PDF";
        }
        if (mime.startsWith("image/") || name.matches(".*\\.(png|jpe?g|gif|bmp|webp|tiff?)$")) {
            return "IMAGE";
        }
        return "UNSUPPORTED";
    }

    public enum Source {
        ATTACHMENT,
        DMS;

        public static Source from(String value) {
            try {
                return Source.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                throw new ValidationException("Unsupported document source");
            }
        }
    }

    public record Metadata(
            String source, UUID id, Integer version, String fileName, String mimeType,
            long fileSize, String viewerType, boolean canDownload, boolean canPrint,
            boolean canShare, Watermark watermark, List<Integer> allowedShareExpiryMinutes,
            List<String> annotationCapabilities) {
    }

    public record SharedMetadata(
            String source, UUID id, Integer version, String fileName, String mimeType,
            long fileSize, String viewerType, List<String> annotationCapabilities,
            Watermark watermark, Instant expiresAt) {
    }

    public record Watermark(boolean enabled, List<String> lines) {
    }

    public record Content(Resource resource, String fileName, String mimeType, long fileSize) {
    }

    public record ShareRequest(Integer expiryMinutes) {
    }

    public record ShareResponse(String token, Instant expiresAt) {
    }
}
