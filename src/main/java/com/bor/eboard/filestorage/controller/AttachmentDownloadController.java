package com.bor.eboard.filestorage.controller;

import com.bor.eboard.documentviewer.service.DocumentViewerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Backward-compatible attachment endpoint. Authorization and auditing are
 * delegated to the centralized secure document viewer.
 */
@RestController
@RequestMapping("/api/v1/files/storage")
@RequiredArgsConstructor
@Tag(name = "File Storage", description = "Attachment download")
public class AttachmentDownloadController {

    private final DocumentViewerService documentViewerService;

    @Operation(summary = "Download an attachment by id")
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID attachmentId) {
        DocumentViewerService.Content downloadable = documentViewerService.download(
                DocumentViewerService.Source.ATTACHMENT, attachmentId, null);
        String encodedName = URLEncoder.encode(
                downloadable.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(downloadable.mimeType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .header("X-Content-Type-Options", "nosniff");
        if (downloadable.fileSize() > 0) {
            response.contentLength(downloadable.fileSize());
        }
        return response.body(downloadable.resource());
    }
}
