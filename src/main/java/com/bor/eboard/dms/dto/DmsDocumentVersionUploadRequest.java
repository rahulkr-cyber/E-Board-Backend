package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.Size;

public record DmsDocumentVersionUploadRequest(
        @Size(max = 1000) String versionComment) {
}
