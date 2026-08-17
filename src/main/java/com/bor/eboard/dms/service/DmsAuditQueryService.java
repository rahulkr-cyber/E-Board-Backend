package com.bor.eboard.dms.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.dms.dto.DmsAuditEventResponse;
import com.bor.eboard.dms.dto.DmsAuditSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface DmsAuditQueryService {

    PageResponse<DmsAuditEventResponse> search(DmsAuditSearchRequest request);

    List<DmsAuditEventResponse> findDocumentAudit(UUID documentId);

    List<DmsDocumentHistoryResponse> findDocumentHistory(UUID documentId);
}
