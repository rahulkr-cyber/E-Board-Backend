package com.bor.eboard.dms.dto;

import java.util.List;
import java.util.UUID;

public record DmsDocumentAccessResponse(
        UUID documentId,
        String documentNumber,
        DmsDocumentAccessSummaryResponse currentUserAccess,
        List<DmsDocumentPermissionResponse> permissions,
        List<DmsDocumentShareResponse> shares) {
}
