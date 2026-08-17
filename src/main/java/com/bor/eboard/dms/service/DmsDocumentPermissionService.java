package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsDocumentAccessGrantRequest;
import com.bor.eboard.dms.dto.DmsDocumentAccessResponse;

import java.util.UUID;

public interface DmsDocumentPermissionService {

    DmsDocumentAccessResponse getAccess(UUID documentId);

    DmsDocumentAccessResponse grant(UUID documentId, DmsDocumentAccessGrantRequest request);

    DmsDocumentAccessResponse revoke(UUID documentId, UUID permissionId);
}
