package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsDocumentMetadataUpdateRequest;
import com.bor.eboard.dms.dto.DmsDocumentResponse;
import com.bor.eboard.dms.dto.DmsDocumentUploadRequest;
import com.bor.eboard.dms.dto.DmsDocumentVersionResponse;
import com.bor.eboard.dms.dto.DmsDocumentVersionUploadRequest;
import com.bor.eboard.dms.dto.DmsUploadPolicyResponse;
import com.bor.eboard.dms.service.DmsDocumentService;
import com.bor.eboard.dms.service.DmsFilePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dms/documents")
@Tag(name = "DMS Documents", description = "DMS document upload, metadata and version management")
public class DmsDocumentController {

    private final DmsDocumentService documentService;
    private final DmsFilePolicyService filePolicyService;

    public DmsDocumentController(
            DmsDocumentService documentService,
            DmsFilePolicyService filePolicyService) {
        this.documentService = documentService;
        this.filePolicyService = filePolicyService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.CREATE + "', '"
            + DmsPermissions.UPLOAD + "', '" + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Upload a new DMS document and version 1")
    public ApiResponse<DmsDocumentResponse> upload(
            @Valid @RequestPart("request") DmsDocumentUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(
                "DMS document uploaded successfully",
                documentService.upload(request, file));
    }


    @GetMapping("/upload-policy")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.UPDATE + "', '" + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Get the effective DMS upload policy")
    public ApiResponse<DmsUploadPolicyResponse> uploadPolicy() {
        return ApiResponse.success(filePolicyService.getPolicy());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.UPDATE + "', '" + DmsPermissions.DOWNLOAD + "', '"
            + DmsPermissions.SHARE + "', '" + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Get DMS document details")
    public ApiResponse<DmsDocumentResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(documentService.findById(id));
    }

    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.UPDATE + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Update DMS document metadata and tags")
    public ApiResponse<DmsDocumentResponse> updateMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody DmsDocumentMetadataUpdateRequest request) {
        return ApiResponse.success(
                "DMS document metadata updated successfully",
                documentService.updateMetadata(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.UPDATE + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Archive a DMS document")
    public ApiResponse<DmsDocumentResponse> archive(@PathVariable UUID id) {
        return ApiResponse.success(
                "DMS document archived successfully",
                documentService.archive(id));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Upload a new immutable DMS document version")
    public ApiResponse<DmsDocumentVersionResponse> uploadVersion(
            @PathVariable UUID id,
            @Valid @RequestPart("request") DmsDocumentVersionUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(
                "DMS document version uploaded successfully",
                documentService.uploadVersion(id, request, file));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.UPDATE + "', '" + DmsPermissions.DOWNLOAD + "', '"
            + DmsPermissions.SHARE + "', '" + DmsPermissions.ADMIN + "')")
    @Operation(summary = "List DMS document versions")
    public ApiResponse<List<DmsDocumentVersionResponse>> findVersions(@PathVariable UUID id) {
        return ApiResponse.success(documentService.findVersions(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.DOWNLOAD + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Download the latest DMS document version")
    public ResponseEntity<Resource> downloadLatest(@PathVariable UUID id) {
        return downloadable(documentService.download(id, null));
    }

    @GetMapping("/{id}/versions/{versionNumber}/download")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.DOWNLOAD + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Download a specific DMS document version")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber) {
        return downloadable(documentService.download(id, versionNumber));
    }

    private ResponseEntity<Resource> downloadable(
            DmsDocumentService.DownloadableDocument downloadable) {
        String encodedName = URLEncoder.encode(
                        downloadable.originalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(downloadable.mimeType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(downloadable.fileSize())
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Download-Options", "noopen")
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("Content-Security-Policy", "sandbox")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(downloadable.resource());
    }
}
