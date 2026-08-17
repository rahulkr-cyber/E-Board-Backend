package com.bor.eboard.dms.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsDocumentSearchRequest;
import com.bor.eboard.dms.dto.DmsDocumentSearchResponse;
import com.bor.eboard.dms.service.DmsSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dms/search")
@Tag(name = "DMS Search", description = "Metadata-driven DMS document search")
public class DmsSearchController {

    private final DmsSearchService searchService;

    public DmsSearchController(DmsSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.DOWNLOAD + "', '" + DmsPermissions.SHARE + "', '"
            + DmsPermissions.AUDIT_VIEW + "', '" + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Search DMS documents by keywords, metadata and configured filters")
    public ApiResponse<PageResponse<DmsDocumentSearchResponse>> search(
            @Valid @RequestBody DmsDocumentSearchRequest request) {
        return ApiResponse.success(searchService.search(request));
    }
}
