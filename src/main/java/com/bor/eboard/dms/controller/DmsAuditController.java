package com.bor.eboard.dms.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsAuditEventResponse;
import com.bor.eboard.dms.dto.DmsAuditSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentHistoryResponse;
import com.bor.eboard.dms.service.DmsAuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dms")
@Tag(name = "DMS Audit and History", description = "Immutable DMS audit and document history")
public class DmsAuditController {

    private final DmsAuditQueryService auditQueryService;

    public DmsAuditController(DmsAuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.AUDIT_VIEW + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Search DMS audit events")
    public ApiResponse<PageResponse<DmsAuditEventResponse>> search(
            @ModelAttribute DmsAuditSearchRequest request) {
        return ApiResponse.success(auditQueryService.search(request));
    }

    @GetMapping("/documents/{documentId}/audit")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.AUDIT_VIEW + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Get audit events for a DMS document")
    public ApiResponse<List<DmsAuditEventResponse>> documentAudit(
            @PathVariable UUID documentId) {
        return ApiResponse.success(auditQueryService.findDocumentAudit(documentId));
    }

    @GetMapping("/documents/{documentId}/history")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPDATE + "', '"
            + DmsPermissions.DOWNLOAD + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.SHARE + "', '" + DmsPermissions.AUDIT_VIEW + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Get immutable history timeline for a DMS document")
    public ApiResponse<List<DmsDocumentHistoryResponse>> documentHistory(
            @PathVariable UUID documentId) {
        return ApiResponse.success(auditQueryService.findDocumentHistory(documentId));
    }
}
