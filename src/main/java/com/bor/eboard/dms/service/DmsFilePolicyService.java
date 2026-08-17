package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsUploadPolicyResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface DmsFilePolicyService {

    ValidatedFile validateUpload(MultipartFile file);

    void validateMetadataPayload(Map<String, Object> metadata);

    boolean verifyStoredContent(Resource resource, long expectedSize, String expectedChecksum);

    DmsUploadPolicyResponse getPolicy();

    record ValidatedFile(
            String originalFileName,
            String extension,
            String mimeType,
            long fileSize,
            String checksumSha256) {
    }
}
