package com.bor.eboard.documentviewer.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.documentviewer.service.DocumentViewerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/document-viewer")
public class DocumentViewerController {

    private final DocumentViewerService viewerService;

    public DocumentViewerController(DocumentViewerService viewerService) {
        this.viewerService = viewerService;
    }

    @GetMapping("/{source}/{id}")
    public ApiResponse<DocumentViewerService.Metadata> metadata(
            @PathVariable String source,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer version,
            HttpServletRequest request) {
        return ApiResponse.success(viewerService.metadata(
                DocumentViewerService.Source.from(source), id, version, request));
    }

    @GetMapping("/{source}/{id}/content")
    public ResponseEntity<Resource> content(
            @PathVariable String source,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer version) {
        return resource(viewerService.view(
                DocumentViewerService.Source.from(source), id, version), "inline");
    }

    @GetMapping("/{source}/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String source,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer version) {
        return resource(viewerService.download(
                DocumentViewerService.Source.from(source), id, version), "attachment");
    }

    @PostMapping("/{source}/{id}/print")
    public ApiResponse<Void> print(
            @PathVariable String source,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer version) {
        viewerService.authorizePrint(DocumentViewerService.Source.from(source), id, version);
        return ApiResponse.successMessage("Print access recorded");
    }

    @PostMapping("/{source}/{id}/share")
    public ApiResponse<DocumentViewerService.ShareResponse> share(
            @PathVariable String source,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer version,
            @RequestBody(required = false) DocumentViewerService.ShareRequest request) {
        Integer expiry = request == null ? null : request.expiryMinutes();
        return ApiResponse.success(viewerService.share(
                DocumentViewerService.Source.from(source), id, version, expiry));
    }

    @GetMapping("/shared/{token}")
    public ApiResponse<DocumentViewerService.SharedMetadata> sharedMetadata(
            @PathVariable String token,
            HttpServletRequest request) {
        return ApiResponse.success(viewerService.sharedMetadata(token, request));
    }

    @GetMapping("/shared/{token}/content")
    public ResponseEntity<Resource> sharedContent(@PathVariable String token) {
        return resource(viewerService.sharedContent(token), "inline");
    }

    private ResponseEntity<Resource> resource(
            DocumentViewerService.Content content, String disposition) {
        String encodedName = URLEncoder.encode(
                content.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(content.mimeType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename*=UTF-8''" + encodedName)
                .header("X-Content-Type-Options", "nosniff");
        if (content.fileSize() > 0) {
            response.contentLength(content.fileSize());
        }
        return response.body(content.resource());
    }
}
