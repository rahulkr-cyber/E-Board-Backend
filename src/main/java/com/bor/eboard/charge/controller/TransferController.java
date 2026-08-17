package com.bor.eboard.charge.controller;

import com.bor.eboard.charge.dto.CreateJoiningRelievingRequest;
import com.bor.eboard.charge.dto.CreateTransferRequest;
import com.bor.eboard.charge.dto.CreatePostingRequest;
import com.bor.eboard.charge.dto.JoiningRelievingResponse;
import com.bor.eboard.charge.dto.PostingResponse;
import com.bor.eboard.charge.dto.PostingTimelineEntryResponse;
import com.bor.eboard.charge.dto.TransferResponse;
import com.bor.eboard.charge.service.JoiningRelievingService;
import com.bor.eboard.charge.service.PostingService;
import com.bor.eboard.charge.service.TransferService;
import com.bor.eboard.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Transfer, joining/relieving, and posting-history APIs
 * (04_API_SPEC.md section 15).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Transfer & Posting", description = "Transfers, joining/relieving, posting history")
public class TransferController {

    private final TransferService transferService;
    private final JoiningRelievingService joiningRelievingService;
    private final PostingService postingService;


    @Operation(summary = "Create a detailed employee posting record")
    @PostMapping("/postings")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<PostingTimelineEntryResponse> createPosting(
            @Valid @RequestBody CreatePostingRequest request) {
        return ApiResponse.success("Posting recorded", postingService.create(request));
    }

    @Operation(summary = "Upload or replace a posting Government Order")
    @PostMapping("/postings/{id}/order-document")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<PostingTimelineEntryResponse> uploadPostingOrderDocument(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Order document uploaded",
                postingService.uploadOrderDocument(id, file));
    }

    @Operation(summary = "Add a supporting document to a posting")
    @PostMapping("/postings/{id}/supporting-documents")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<PostingTimelineEntryResponse> addPostingSupportingDocument(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Supporting document uploaded",
                postingService.addSupportingDocument(id, file));
    }

    @Operation(summary = "Record a transfer")
    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<TransferResponse> transfer(
            @Valid @RequestBody CreateTransferRequest request) {
        return ApiResponse.success("Transfer recorded", transferService.record(request));
    }

    @Operation(summary = "Transfer history for a user")
    @GetMapping("/transfers/user/{userId}")
    @PreAuthorize("hasAuthority('POSTING_VIEW') or hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<List<TransferResponse>> transfersByUser(@PathVariable UUID userId) {
        return ApiResponse.success(transferService.getByUser(userId));
    }

    @Operation(summary = "Record a joining or relieving event")
    @PostMapping("/joining-relieving")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<JoiningRelievingResponse> joiningRelieving(
            @Valid @RequestBody CreateJoiningRelievingRequest request) {
        return ApiResponse.success("Event recorded", joiningRelievingService.record(request));
    }

    @Operation(summary = "Joining/relieving history for a user")
    @GetMapping("/joining-relieving/user/{userId}")
    @PreAuthorize("hasAuthority('POSTING_VIEW') or hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<List<JoiningRelievingResponse>> eventsByUser(@PathVariable UUID userId) {
        return ApiResponse.success(joiningRelievingService.getByUser(userId));
    }

    @Operation(summary = "Consolidated posting history for a user")
    @GetMapping("/postings/user/{userId}")
    @PreAuthorize("hasAuthority('POSTING_VIEW') or hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<PostingResponse> posting(@PathVariable UUID userId) {
        return ApiResponse.success(joiningRelievingService.getPostingHistory(userId));
    }

    /**
     * Upload — or replace — the Government Order / Office Order PDF for a
     * transfer. Downloadable afterwards through the existing
     * {@code GET /api/v1/files/storage/{attachmentId}/download} endpoint.
     */
    @Operation(summary = "Upload or replace the transfer order document")
    @PostMapping("/transfers/{id}/order-document")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<TransferResponse> uploadTransferOrderDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Order document uploaded",
                transferService.uploadOrderDocument(id, file));
    }

    /** Upload — or replace — the order document for a joining/relieving event. */
    @Operation(summary = "Upload or replace the joining/relieving order document")
    @PostMapping("/joining-relieving/{id}/order-document")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public ApiResponse<JoiningRelievingResponse> uploadEventOrderDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Order document uploaded",
                joiningRelievingService.uploadOrderDocument(id, file));
    }
}
