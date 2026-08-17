package com.bor.eboard.dms.service.impl;

import com.bor.eboard.dms.service.DmsAuditTrailService;
import com.bor.eboard.common.exception.FileStorageException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.dms.document.DmsDocumentStatus;
import com.bor.eboard.dms.dto.DmsDocumentMetadataUpdateRequest;
import com.bor.eboard.dms.dto.DmsDocumentResponse;
import com.bor.eboard.dms.dto.DmsDocumentUploadRequest;
import com.bor.eboard.dms.dto.DmsDocumentVersionResponse;
import com.bor.eboard.dms.dto.DmsDocumentVersionUploadRequest;
import com.bor.eboard.dms.dto.DmsMetadataValidationResponse;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentMetadata;
import com.bor.eboard.dms.entity.DmsDocumentTag;
import com.bor.eboard.dms.entity.DmsDocumentType;
import com.bor.eboard.dms.entity.DmsDocumentVersion;
import com.bor.eboard.dms.entity.DmsMetadataField;
import com.bor.eboard.dms.metadata.DmsMetadataControlType;
import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.repository.DmsDocumentMetadataRepository;
import com.bor.eboard.dms.repository.DmsDocumentRepository;
import com.bor.eboard.dms.repository.DmsDocumentTagRepository;
import com.bor.eboard.dms.repository.DmsDocumentTypeRepository;
import com.bor.eboard.dms.repository.DmsDocumentVersionRepository;
import com.bor.eboard.dms.repository.DmsMetadataFieldRepository;
import com.bor.eboard.dms.service.DmsDocumentAuthorizationService;
import com.bor.eboard.dms.service.DmsFilePolicyService;
import com.bor.eboard.dms.service.DmsDocumentService;
import com.bor.eboard.dms.service.DmsMetadataValidationService;
import com.bor.eboard.dms.service.DmsSearchIndexService;
import com.bor.eboard.dms.storage.StorageProvider;
import com.bor.eboard.dms.storage.StorageProviderFactory;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DmsDocumentServiceImpl implements DmsDocumentService {

    private static final String ENTITY_TYPE = "DOCUMENT";
    private static final int MAX_TAGS = 50;

    private final DmsDocumentRepository documentRepository;
    private final DmsDocumentVersionRepository versionRepository;
    private final DmsDocumentMetadataRepository metadataRepository;
    private final DmsDocumentTagRepository tagRepository;
    private final DmsDocumentTypeRepository documentTypeRepository;
    private final DmsMetadataFieldRepository metadataFieldRepository;
    private final DmsMetadataValidationService metadataValidationService;
    private final StorageProviderFactory storageProviderFactory;
    private final DmsDocumentNumberGenerator documentNumberGenerator;
    private final DmsSearchIndexService searchIndexService;
    private final DmsDocumentAuthorizationService authorizationService;
    private final DmsFilePolicyService filePolicyService;
    private final UserRepository userRepository;
    private final DmsAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public DmsDocumentServiceImpl(
            DmsDocumentRepository documentRepository,
            DmsDocumentVersionRepository versionRepository,
            DmsDocumentMetadataRepository metadataRepository,
            DmsDocumentTagRepository tagRepository,
            DmsDocumentTypeRepository documentTypeRepository,
            DmsMetadataFieldRepository metadataFieldRepository,
            DmsMetadataValidationService metadataValidationService,
            StorageProviderFactory storageProviderFactory,
            DmsDocumentNumberGenerator documentNumberGenerator,
            DmsSearchIndexService searchIndexService,
            DmsDocumentAuthorizationService authorizationService,
            DmsFilePolicyService filePolicyService,
            UserRepository userRepository,
            DmsAuditTrailService auditTrailService,
            ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.metadataRepository = metadataRepository;
        this.tagRepository = tagRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.metadataFieldRepository = metadataFieldRepository;
        this.metadataValidationService = metadataValidationService;
        this.storageProviderFactory = storageProviderFactory;
        this.documentNumberGenerator = documentNumberGenerator;
        this.searchIndexService = searchIndexService;
        this.authorizationService = authorizationService;
        this.filePolicyService = filePolicyService;
        this.userRepository = userRepository;
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DmsDocumentResponse upload(
            DmsDocumentUploadRequest request,
            MultipartFile file) {
        User uploader = currentUser();
        DmsDocumentType documentType = requireActiveDocumentType(request.documentTypeId());
        DmsFilePolicyService.ValidatedFile validatedFile = filePolicyService.validateUpload(file);

        Map<String, Object> values = prepareMetadataValues(
                documentType.getId(), request.metadata(), Collections.emptyMap(),
                validatedFile.originalFileName());
        validateMetadata(documentType.getId(), values);
        filePolicyService.validateMetadataPayload(values);
        List<String> tags = normalizeTags(request.tags());

        StorageProvider provider = storageProviderFactory.currentProvider();
        String storageKey = store(provider, file, validatedFile);
        registerRollbackCleanup(provider, storageKey);

        LocalDateTime now = LocalDateTime.now();
        DmsDocument document = new DmsDocument();
        document.setDocumentTypeId(documentType.getId());
        document.setDocumentNumber(documentNumberGenerator.next());
        document.setTitle(normalizeRequired(request.title()));
        document.setDescription(normalizeOptional(request.description()));
        document.setStatus(DmsDocumentStatus.ACTIVE);
        document.setCurrentVersionNumber(1);
        document.setUploadedBy(uploader.getId());
        document.setDepartmentId(uploader.getDepartmentId());
        document.setSectionId(uploader.getSectionId());
        document.setUploadedAt(now);
        document = documentRepository.saveAndFlush(document);

        DmsDocumentVersion version = newVersion(
                document.getId(), 1, provider.providerCode(), storageKey,
                validatedFile, request.versionComment(), uploader.getId(), now);
        version = versionRepository.saveAndFlush(version);
        replaceMetadata(document, values);
        replaceTags(document.getId(), tags);
        searchIndexService.refresh(document.getId());

        auditTrailService.recordDocumentHistory(
                document.getId(), ENTITY_TYPE, document.getId(),
                "UPLOAD", null,
                document.getDocumentNumber() + ";version=1;file=" + version.getOriginalFileName(),
                "Document uploaded", 1,
                historySnapshot(document, values, tags, version));
        return toResponse(document, version);
    }

    @Override
    @Transactional(readOnly = true)
    public DmsDocumentResponse findById(UUID id) {
        DmsDocument document = findDocument(id);
        authorizationService.require(document, DmsDocumentAccessLevel.VIEW);
        return toResponse(document, latestVersion(document.getId()));
    }

    @Override
    @Transactional
    public DmsDocumentResponse updateMetadata(
            UUID id,
            DmsDocumentMetadataUpdateRequest request) {
        DmsDocument document = findDocumentForUpdate(id);
        authorizationService.require(document, DmsDocumentAccessLevel.UPDATE);
        requireActive(document);

        Map<String, Object> existing = readMetadata(document.getId());
        Map<String, Object> values = prepareMetadataValues(
                document.getDocumentTypeId(), request.metadata(), existing, null);
        validateMetadata(document.getDocumentTypeId(), values);
        filePolicyService.validateMetadataPayload(values);
        List<String> tags = normalizeTags(request.tags());

        String oldAudit = auditValue(document, existing,
                tagRepository.findByDocumentIdAndDeletedFalseOrderByTagValueAsc(document.getId())
                        .stream().map(DmsDocumentTag::getTagValue).toList());
        document.setTitle(normalizeRequired(request.title()));
        document.setDescription(normalizeOptional(request.description()));
        document = documentRepository.save(document);
        replaceMetadata(document, values);
        replaceTags(document.getId(), tags);
        searchIndexService.refresh(document.getId());

        DmsDocumentVersion latestVersion = latestVersion(document.getId());
        auditTrailService.recordDocumentHistory(
                document.getId(), ENTITY_TYPE, document.getId(),
                "METADATA_UPDATE", oldAudit, auditValue(document, values, tags),
                "Document metadata updated", document.getCurrentVersionNumber(),
                historySnapshot(document, values, tags, latestVersion));
        return toResponse(document, latestVersion);
    }

    @Override
    @Transactional
    public DmsDocumentResponse archive(UUID id) {
        DmsDocument document = findDocumentForUpdate(id);
        authorizationService.require(document, DmsDocumentAccessLevel.UPDATE);

        if (document.getStatus() == DmsDocumentStatus.ARCHIVED) {
            return toResponse(document, latestVersion(document.getId()));
        }

        String previousStatus = document.getStatus().name();
        document.setStatus(DmsDocumentStatus.ARCHIVED);
        document = documentRepository.save(document);
        searchIndexService.refresh(document.getId());

        DmsDocumentVersion latestVersion = latestVersion(document.getId());
        auditTrailService.recordDocumentHistory(
                document.getId(), ENTITY_TYPE, document.getId(),
                "ARCHIVE", "status=" + previousStatus, "status=ARCHIVED",
                "Document archived", document.getCurrentVersionNumber(),
                historySnapshot(document, readMetadata(document.getId()),
                        tagRepository.findByDocumentIdAndDeletedFalseOrderByTagValueAsc(document.getId())
                                .stream().map(DmsDocumentTag::getTagValue).toList(),
                        latestVersion));
        return toResponse(document, latestVersion);
    }

    @Override
    @Transactional
    public DmsDocumentVersionResponse uploadVersion(
            UUID id,
            DmsDocumentVersionUploadRequest request,
            MultipartFile file) {
        DmsDocument document = findDocumentForUpdate(id);
        authorizationService.require(document, DmsDocumentAccessLevel.UPLOAD_VERSION);
        requireActive(document);
        DmsFilePolicyService.ValidatedFile validatedFile = filePolicyService.validateUpload(file);
        User uploader = currentUser();

        int nextVersion = document.getCurrentVersionNumber() + 1;
        StorageProvider provider = storageProviderFactory.currentProvider();
        String storageKey = store(provider, file, validatedFile);
        registerRollbackCleanup(provider, storageKey);

        DmsDocumentVersion version = newVersion(
                document.getId(), nextVersion, provider.providerCode(), storageKey,
                validatedFile, request.versionComment(), uploader.getId(), LocalDateTime.now());
        version = versionRepository.saveAndFlush(version);
        document.setCurrentVersionNumber(nextVersion);
        documentRepository.save(document);
        searchIndexService.refresh(document.getId());

        auditTrailService.recordDocumentHistory(
                document.getId(), ENTITY_TYPE, document.getId(),
                "VERSION_UPLOAD", "version=" + (nextVersion - 1),
                "version=" + nextVersion + ";file=" + version.getOriginalFileName(),
                "Document version " + nextVersion + " uploaded", nextVersion,
                historySnapshot(document, readMetadata(document.getId()),
                        tagRepository.findByDocumentIdAndDeletedFalseOrderByTagValueAsc(document.getId())
                                .stream().map(DmsDocumentTag::getTagValue).toList(),
                        version));
        return toVersionResponse(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DmsDocumentVersionResponse> findVersions(UUID id) {
        DmsDocument document = findDocument(id);
        authorizationService.require(document, DmsDocumentAccessLevel.VIEW);
        return versionRepository
                .findByDocumentIdAndDeletedFalseOrderByVersionNumberDesc(document.getId())
                .stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadableDocument view(UUID id, Integer versionNumber) {
        return loadContent(id, versionNumber, DmsDocumentAccessLevel.VIEW, "VIEW");
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadableDocument download(UUID id, Integer versionNumber) {
        return loadContent(id, versionNumber, DmsDocumentAccessLevel.DOWNLOAD, "DOWNLOAD");
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadableDocument sharedView(UUID id, Integer versionNumber) {
        return loadContent(id, versionNumber, null, "SHARED_VIEW");
    }

    private DownloadableDocument loadContent(
            UUID id, Integer versionNumber, DmsDocumentAccessLevel accessLevel, String auditAction) {
        DmsDocument document = findDocument(id);
        if (accessLevel != null) {
            authorizationService.require(document, accessLevel);
        }
        DmsDocumentVersion version = versionNumber == null
                ? latestVersion(document.getId())
                : versionRepository
                        .findByDocumentIdAndVersionNumberAndDeletedFalse(document.getId(), versionNumber)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "DMS document version", versionNumber));

        StorageProvider provider = storageProviderFactory.provider(version.getStorageProvider());
        Resource resource = provider.load(version.getStorageKey());
        boolean verified = filePolicyService.verifyStoredContent(
                resource, version.getFileSize(), version.getChecksumSha256());
        if (verified) {
            resource = provider.load(version.getStorageKey());
        }
        auditTrailService.recordDocumentAudit(
                document.getId(), ENTITY_TYPE, document.getId(),
                auditAction, null, "version=" + version.getVersionNumber());
        return new DownloadableDocument(
                resource,
                version.getOriginalFileName(),
                version.getMimeType(),
                version.getFileSize());
    }

    private DmsDocumentVersion newVersion(
            UUID documentId,
            int versionNumber,
            String providerCode,
            String storageKey,
            DmsFilePolicyService.ValidatedFile validatedFile,
            String versionComment,
            UUID uploadedBy,
            LocalDateTime uploadedAt) {
        DmsDocumentVersion version = new DmsDocumentVersion();
        version.setDocumentId(documentId);
        version.setVersionNumber(versionNumber);
        version.setStorageProvider(providerCode);
        version.setStorageKey(storageKey);
        version.setOriginalFileName(validatedFile.originalFileName());
        version.setMimeType(validatedFile.mimeType());
        version.setFileSize(validatedFile.fileSize());
        version.setChecksumSha256(validatedFile.checksumSha256());
        version.setVersionComment(normalizeOptional(versionComment));
        version.setUploadedBy(uploadedBy);
        version.setUploadedAt(uploadedAt);
        return version;
    }

    private void replaceMetadata(DmsDocument document, Map<String, Object> values) {
        List<DmsDocumentMetadata> existing = metadataRepository
                .findByDocumentIdAndDeletedFalseOrderByFieldLabelAsc(document.getId());
        if (!existing.isEmpty()) {
            existing.forEach(item -> item.setDeleted(Boolean.TRUE));
            metadataRepository.saveAllAndFlush(existing);
        }

        List<DmsMetadataField> fields = metadataFieldRepository
                .findByDocumentTypeIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscLabelAsc(
                        document.getDocumentTypeId());
        List<DmsDocumentMetadata> replacements = new ArrayList<>();
        for (DmsMetadataField field : fields) {
            Object value = findValue(values, field.getFieldKey());
            if (isEmpty(value) && field.getDefaultValue() != null) {
                value = field.getDefaultValue();
            }
            if (isEmpty(value)) {
                continue;
            }
            DmsDocumentMetadata metadata = new DmsDocumentMetadata();
            metadata.setDocumentId(document.getId());
            metadata.setMetadataFieldId(field.getId());
            metadata.setFieldKey(field.getFieldKey());
            metadata.setFieldLabel(field.getLabel());
            metadata.setValueJson(writeJson(value));
            metadata.setSearchable(Boolean.TRUE.equals(field.getSearchable()));
            replacements.add(metadata);
        }
        if (!replacements.isEmpty()) {
            metadataRepository.saveAll(replacements);
        }
    }

    private void replaceTags(UUID documentId, List<String> tags) {
        List<DmsDocumentTag> existing = tagRepository
                .findByDocumentIdAndDeletedFalseOrderByTagValueAsc(documentId);
        if (!existing.isEmpty()) {
            existing.forEach(item -> item.setDeleted(Boolean.TRUE));
            tagRepository.saveAllAndFlush(existing);
        }
        if (tags.isEmpty()) {
            return;
        }
        List<DmsDocumentTag> replacements = tags.stream().map(tag -> {
            DmsDocumentTag entity = new DmsDocumentTag();
            entity.setDocumentId(documentId);
            entity.setTagValue(tag);
            return entity;
        }).toList();
        tagRepository.saveAll(replacements);
    }

    private Map<String, Object> prepareMetadataValues(
            UUID documentTypeId,
            Map<String, Object> submitted,
            Map<String, Object> existing,
            String uploadedFileName) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (submitted != null) {
            submitted.forEach((key, value) -> {
                if (key != null && !key.isBlank()) {
                    values.put(key.trim(), value);
                }
            });
        }

        List<DmsMetadataField> fields = metadataFieldRepository
                .findByDocumentTypeIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscLabelAsc(
                        documentTypeId);
        for (DmsMetadataField field : fields) {
            if (field.getControlType() != DmsMetadataControlType.FILE_UPLOAD) {
                continue;
            }
            Object submittedValue = findValue(values, field.getFieldKey());
            if (!isEmpty(submittedValue)) {
                continue;
            }
            if (uploadedFileName != null) {
                values.put(field.getFieldKey(), uploadedFileName);
                continue;
            }
            Object existingValue = findValue(existing, field.getFieldKey());
            if (!isEmpty(existingValue)) {
                values.put(field.getFieldKey(), existingValue);
            }
        }
        return values;
    }

    private void validateMetadata(UUID documentTypeId, Map<String, Object> values) {
        DmsMetadataValidationResponse result = metadataValidationService.validate(documentTypeId, values);
        if (result.valid()) {
            return;
        }
        String message = result.errors().stream()
                .map(error -> error.label() + ": " + error.message())
                .collect(Collectors.joining("; "));
        throw new ValidationException("DMS metadata validation failed: " + message);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String rawTag : tags) {
            if (rawTag == null || rawTag.isBlank()) {
                continue;
            }
            String tag = rawTag.trim();
            if (tag.length() > 100) {
                throw new ValidationException("DMS tag cannot exceed 100 characters");
            }
            unique.putIfAbsent(tag.toLowerCase(Locale.ROOT), tag);
        }
        if (unique.size() > MAX_TAGS) {
            throw new ValidationException("A DMS document can contain at most " + MAX_TAGS + " tags");
        }
        return List.copyOf(unique.values());
    }

    private Map<String, Object> historySnapshot(
            DmsDocument document,
            Map<String, Object> metadata,
            List<String> tags,
            DmsDocumentVersion version) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("documentNumber", document.getDocumentNumber());
        snapshot.put("title", document.getTitle());
        snapshot.put("description", document.getDescription());
        snapshot.put("status", document.getStatus().name());
        snapshot.put("currentVersionNumber", document.getCurrentVersionNumber());
        snapshot.put("metadata", new LinkedHashMap<>(metadata));
        snapshot.put("tags", List.copyOf(tags));
        if (version != null) {
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("versionNumber", version.getVersionNumber());
            file.put("originalFileName", version.getOriginalFileName());
            file.put("mimeType", version.getMimeType());
            file.put("fileSize", version.getFileSize());
            file.put("checksumSha256", version.getChecksumSha256());
            file.put("versionComment", version.getVersionComment());
            snapshot.put("file", file);
        }
        return snapshot;
    }

    private DmsDocumentResponse toResponse(
            DmsDocument document,
            DmsDocumentVersion latestVersion) {
        DmsDocumentType documentType = documentTypeRepository
                .findById(document.getDocumentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DMS document type", document.getDocumentTypeId()));
        User uploader = userRepository.findByIdAndDeletedFalse(document.getUploadedBy()).orElse(null);
        Map<String, Object> metadata = readMetadata(document.getId());
        List<String> tags = tagRepository
                .findByDocumentIdAndDeletedFalseOrderByTagValueAsc(document.getId())
                .stream().map(DmsDocumentTag::getTagValue).toList();
        return new DmsDocumentResponse(
                document.getId(),
                document.getDocumentNumber(),
                document.getDocumentTypeId(),
                documentType.getCode(),
                documentType.getName(),
                document.getTitle(),
                document.getDescription(),
                document.getStatus().name(),
                document.getCurrentVersionNumber(),
                document.getUploadedBy(),
                uploader == null ? null : uploader.getFullName(),
                document.getDepartmentId(),
                document.getSectionId(),
                document.getUploadedAt(),
                document.getUpdatedAt(),
                metadata,
                tags,
                toVersionResponse(latestVersion),
                authorizationService.summarize(document));
    }

    private DmsDocumentVersionResponse toVersionResponse(DmsDocumentVersion version) {
        User uploader = userRepository.findByIdAndDeletedFalse(version.getUploadedBy()).orElse(null);
        return new DmsDocumentVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getMimeType(),
                version.getFileSize(),
                version.getChecksumSha256(),
                version.getVersionComment(),
                version.getUploadedBy(),
                uploader == null ? null : uploader.getFullName(),
                version.getUploadedAt());
    }

    private Map<String, Object> readMetadata(UUID documentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        metadataRepository.findByDocumentIdAndDeletedFalseOrderByFieldLabelAsc(documentId)
                .forEach(item -> result.put(item.getFieldKey(), readJson(item.getValueJson())));
        return Collections.unmodifiableMap(result);
    }

    private DmsDocument findDocument(UUID id) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document", id));
    }

    private DmsDocument findDocumentForUpdate(UUID id) {
        return documentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document", id));
    }

    private void requireActive(DmsDocument document) {
        if (document.getStatus() == DmsDocumentStatus.ARCHIVED) {
            throw new ValidationException("Archived DMS documents are read-only");
        }
    }

    private DmsDocumentVersion latestVersion(UUID documentId) {
        return versionRepository.findFirstByDocumentIdAndDeletedFalseOrderByVersionNumberDesc(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DMS document version", documentId));
    }

    private DmsDocumentType requireActiveDocumentType(UUID id) {
        DmsDocumentType type = documentTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document type", id));
        if (!Boolean.TRUE.equals(type.getActive())) {
            throw new ValidationException("Inactive DMS document types cannot accept uploads");
        }
        return type;
    }

    private User currentUser() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authenticated user is required"));
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user was not found"));
    }

    private String store(
            StorageProvider provider,
            MultipartFile file,
            DmsFilePolicyService.ValidatedFile validatedFile) {
        try (InputStream inputStream = file.getInputStream()) {
            return provider.store(
                    inputStream,
                    validatedFile.fileSize(),
                    validatedFile.originalFileName(),
                    validatedFile.mimeType());
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store DMS document", ex);
        }
    }

    private void registerRollbackCleanup(StorageProvider provider, String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    provider.delete(storageKey);
                } catch (IOException ignored) {
                    // The storage exception has already caused the business transaction to fail.
                }
            }
        });
    }

    private Object findValue(Map<String, Object> values, String key) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Iterable<?> iterable) {
            return !iterable.iterator().hasNext();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) == 0;
        }
        return false;
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("DMS document title is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("DMS metadata value cannot be serialized");
        }
    }

    private Object readJson(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored DMS metadata is invalid", ex);
        }
    }

    private String auditValue(
            DmsDocument document,
            Map<String, Object> metadata,
            List<String> tags) {
        return "number=" + document.getDocumentNumber()
                + ";title=" + document.getTitle()
                + ";metadataKeys=" + new LinkedHashSet<>(metadata.keySet())
                + ";tags=" + tags;
    }
}
