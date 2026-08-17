package com.bor.eboard.dms.dto;

import java.util.List;

public record DmsUploadPolicyResponse(
        long maxFileSizeBytes,
        int maxMetadataBytes,
        List<String> allowedExtensions,
        List<String> allowedMimeTypes,
        boolean verifyChecksumOnDownload) {
}
