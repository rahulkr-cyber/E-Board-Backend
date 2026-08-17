package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsDocumentMetadataUpdateRequest;
import com.bor.eboard.dms.dto.DmsDocumentResponse;
import com.bor.eboard.dms.dto.DmsDocumentUploadRequest;
import com.bor.eboard.dms.dto.DmsDocumentVersionResponse;
import com.bor.eboard.dms.dto.DmsDocumentVersionUploadRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DmsDocumentService {

    DmsDocumentResponse upload(DmsDocumentUploadRequest request, MultipartFile file);

    DmsDocumentResponse findById(UUID id);

    DmsDocumentResponse updateMetadata(UUID id, DmsDocumentMetadataUpdateRequest request);

    DmsDocumentResponse archive(UUID id);

    DmsDocumentVersionResponse uploadVersion(
            UUID id,
            DmsDocumentVersionUploadRequest request,
            MultipartFile file);

    List<DmsDocumentVersionResponse> findVersions(UUID id);

    DownloadableDocument view(UUID id, Integer versionNumber);

    DownloadableDocument download(UUID id, Integer versionNumber);

    /** Loads content after a signed temporary share token has been validated. */
    DownloadableDocument sharedView(UUID id, Integer versionNumber);

    record DownloadableDocument(
            Resource resource,
            String originalFileName,
            String mimeType,
            long fileSize) {
    }
}
