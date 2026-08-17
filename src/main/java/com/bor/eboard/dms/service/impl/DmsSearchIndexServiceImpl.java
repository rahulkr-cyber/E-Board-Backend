package com.bor.eboard.dms.service.impl;

import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.dms.entity.DmsDocument;
import com.bor.eboard.dms.entity.DmsDocumentMetadata;
import com.bor.eboard.dms.entity.DmsDocumentTag;
import com.bor.eboard.dms.entity.DmsDocumentType;
import com.bor.eboard.dms.entity.DmsDocumentVersion;
import com.bor.eboard.dms.entity.DmsSearchIndex;
import com.bor.eboard.dms.repository.DmsDocumentMetadataRepository;
import com.bor.eboard.dms.repository.DmsDocumentRepository;
import com.bor.eboard.dms.repository.DmsDocumentTagRepository;
import com.bor.eboard.dms.repository.DmsDocumentTypeRepository;
import com.bor.eboard.dms.repository.DmsDocumentVersionRepository;
import com.bor.eboard.dms.repository.DmsSearchIndexRepository;
import com.bor.eboard.dms.service.DmsSearchIndexService;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DmsSearchIndexServiceImpl implements DmsSearchIndexService {

    private final DmsSearchIndexRepository searchIndexRepository;
    private final DmsDocumentRepository documentRepository;
    private final DmsDocumentTypeRepository documentTypeRepository;
    private final DmsDocumentMetadataRepository metadataRepository;
    private final DmsDocumentTagRepository tagRepository;
    private final DmsDocumentVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public DmsSearchIndexServiceImpl(
            DmsSearchIndexRepository searchIndexRepository,
            DmsDocumentRepository documentRepository,
            DmsDocumentTypeRepository documentTypeRepository,
            DmsDocumentMetadataRepository metadataRepository,
            DmsDocumentTagRepository tagRepository,
            DmsDocumentVersionRepository versionRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.searchIndexRepository = searchIndexRepository;
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.metadataRepository = metadataRepository;
        this.tagRepository = tagRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void refresh(UUID documentId) {
        DmsDocument document = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DMS document", documentId));
        DmsDocumentType documentType = documentTypeRepository
                .findById(document.getDocumentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DMS document type", document.getDocumentTypeId()));
        User uploader = userRepository.findByIdAndDeletedFalse(document.getUploadedBy()).orElse(null);
        DmsDocumentVersion latestVersion = versionRepository
                .findFirstByDocumentIdAndDeletedFalseOrderByVersionNumberDesc(documentId)
                .orElse(null);

        Map<String, Object> metadata = new LinkedHashMap<>();
        List<String> metadataParts = new ArrayList<>();
        for (DmsDocumentMetadata item : metadataRepository
                .findByDocumentIdAndDeletedFalseOrderByFieldLabelAsc(documentId)) {
            if (!Boolean.TRUE.equals(item.getSearchable())) {
                continue;
            }
            Object value = readJson(item.getValueJson());
            metadata.put(item.getFieldKey(), value);
            appendText(metadataParts, item.getFieldLabel());
            flatten(value, metadataParts);
        }
        List<String> tags = tagRepository
                .findByDocumentIdAndDeletedFalseOrderByTagValueAsc(documentId)
                .stream().map(DmsDocumentTag::getTagValue).toList();

        DmsSearchIndex index = searchIndexRepository
                .findByDocumentIdAndDeletedFalse(documentId)
                .orElseGet(() -> {
                    DmsSearchIndex created = new DmsSearchIndex();
                    created.setId(documentId);
                    created.setDocumentId(documentId);
                    return created;
                });
        index.setDocumentNumber(document.getDocumentNumber());
        index.setDocumentTypeId(documentType.getId());
        index.setDocumentTypeCode(documentType.getCode());
        index.setDocumentTypeName(documentType.getName());
        index.setTitle(document.getTitle());
        index.setDescription(document.getDescription());
        index.setStatus(document.getStatus().name());
        index.setCurrentVersionNumber(document.getCurrentVersionNumber());
        index.setUploadedBy(document.getUploadedBy());
        index.setUploadedByName(uploader == null ? null : uploader.getFullName());
        index.setDepartmentId(document.getDepartmentId());
        index.setSectionId(document.getSectionId());
        index.setUploadedAt(document.getUploadedAt());
        index.setDocumentUpdatedAt(document.getUpdatedAt());
        index.setMetadataJson(writeJson(metadata));
        index.setTagsJson(writeJson(tags));
        index.setMetadataText(String.join(" ", metadataParts));
        index.setTagsText(String.join(" ", tags));
        index.setLatestFileName(latestVersion == null ? null : latestVersion.getOriginalFileName());
        index.setKeywordsText(keywords(document, documentType, uploader, latestVersion, tags, metadataParts));
        index.setDeleted(Boolean.FALSE);
        searchIndexRepository.save(index);
    }

    private String keywords(
            DmsDocument document,
            DmsDocumentType documentType,
            User uploader,
            DmsDocumentVersion latestVersion,
            List<String> tags,
            List<String> metadataParts) {
        List<String> values = new ArrayList<>();
        appendText(values, document.getDocumentNumber());
        appendText(values, document.getTitle());
        appendText(values, document.getDescription());
        appendText(values, document.getStatus().name());
        appendText(values, documentType.getCode());
        appendText(values, documentType.getName());
        appendText(values, uploader == null ? null : uploader.getFullName());
        if (latestVersion != null) {
            appendText(values, latestVersion.getOriginalFileName());
            appendText(values, latestVersion.getVersionComment());
        }
        values.addAll(tags);
        values.addAll(metadataParts);
        return String.join(" ", values);
    }

    private void flatten(Object value, List<String> parts) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> {
                appendText(parts, key == null ? null : String.valueOf(key));
                flatten(item, parts);
            });
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> flatten(item, parts));
            return;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                flatten(java.lang.reflect.Array.get(value, i), parts);
            }
            return;
        }
        appendText(parts, String.valueOf(value));
    }

    private void appendText(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private Object readJson(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored DMS metadata is invalid", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("DMS search index value cannot be serialized", ex);
        }
    }
}
