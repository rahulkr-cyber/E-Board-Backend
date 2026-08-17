package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsDocumentAccessResponse;
import com.bor.eboard.dms.dto.DmsDocumentShareRequest;

import java.util.UUID;

public interface DmsDocumentShareService {

    DmsDocumentAccessResponse share(UUID documentId, DmsDocumentShareRequest request);

    DmsDocumentAccessResponse revoke(UUID documentId, UUID shareId);
}
