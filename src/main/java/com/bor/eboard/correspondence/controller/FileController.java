package com.bor.eboard.correspondence.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.correspondence.dto.CloseFileRequest;
import com.bor.eboard.correspondence.dto.ChangePriorityRequest;
import com.bor.eboard.correspondence.dto.CreateFileRequest;
import com.bor.eboard.correspondence.dto.MarkFileRequest;
import com.bor.eboard.correspondence.dto.PriorityChangeResponse;
import com.bor.eboard.correspondence.dto.ReopenFileRequest;
import com.bor.eboard.correspondence.dto.ReopenHistoryResponse;
import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.CreateReminderRequest;
import com.bor.eboard.correspondence.dto.FileResponse;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.LetterResponse;
import com.bor.eboard.correspondence.dto.ReminderResponse;
import com.bor.eboard.correspondence.dto.TimelineEntry;
import com.bor.eboard.correspondence.service.FileService;
import com.bor.eboard.correspondence.service.FollowupService;
import com.bor.eboard.correspondence.service.LetterService;
import com.bor.eboard.correspondence.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * File APIs (04_API_SPEC.md section 7). Followups and reminders hang off a
 * file, so they are exposed as sub-resources here.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Government files, timeline, followups, reminders")
public class FileController {

    private final FileService fileService;
    private final LetterService letterService;
    private final FollowupService followupService;
    private final ReminderService reminderService;

    @Operation(summary = "Create file (generates immutable file number)")
    @PostMapping
    @PreAuthorize("hasAuthority('FILE_CREATE')")
    public ApiResponse<FileResponse> create(@Valid @RequestBody CreateFileRequest request) {
        return ApiResponse.success("File created successfully", fileService.create(request));
    }

    @Operation(summary = "Get file with letters and attachments")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<FileResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(fileService.getById(id));
    }

    @Operation(summary = "Search files")
    @GetMapping
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<PageResponse<FileResponse>> search(
            @RequestParam(required = false) String fileNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationRequest pagination = new PaginationRequest(page, size, sortBy, sortDir);
        return ApiResponse.success(
                fileService.search(fileNumber, status, sectionId, ownerId, pagination));
    }

    @Operation(summary = "Close a file")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('FILE_CLOSE')")
    public ApiResponse<FileResponse> close(@PathVariable UUID id,
                                           @RequestBody CloseFileRequest request) {
        return ApiResponse.success("File closed successfully", fileService.close(id, request));
    }

    @Operation(summary = "Get file timeline")
    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<List<TimelineEntry>> timeline(@PathVariable UUID id) {
        return ApiResponse.success(fileService.timeline(id));
    }

    @Operation(summary = "Upload file attachment (multipart)")
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('FILE_CREATE')")
    public ApiResponse<AttachmentInfo> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID documentTypeId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Attachment uploaded successfully",
                fileService.uploadAttachment(id, documentTypeId, file));
    }

    // ---- letters belonging to this file ----

    @Operation(summary = "List letters in a file")
    @GetMapping("/{id}/letters")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ApiResponse<List<LetterResponse>> listLetters(@PathVariable UUID id) {
        return ApiResponse.success(letterService.listByFile(id));
    }

    // ---- followups ----

    @Operation(summary = "Create a follow-up on a file")
    @PostMapping("/{id}/followups")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<FollowupResponse> createFollowup(
            @PathVariable UUID id, @Valid @RequestBody CreateFollowupRequest request) {
        return ApiResponse.success("Follow-up created successfully",
                followupService.create(id, request));
    }

    @Operation(summary = "List follow-ups on a file")
    @GetMapping("/{id}/followups")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<List<FollowupResponse>> listFollowups(@PathVariable UUID id) {
        return ApiResponse.success(followupService.listByFile(id));
    }

    // ---- reminders ----

    @Operation(summary = "Create a reminder on a file")
    @PostMapping("/{id}/reminders")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<ReminderResponse> createReminder(
            @PathVariable UUID id, @Valid @RequestBody CreateReminderRequest request) {
        return ApiResponse.success("Reminder created successfully",
                reminderService.create(id, request));
    }

    @Operation(summary = "List reminders on a file")
    @GetMapping("/{id}/reminders")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<List<ReminderResponse>> listReminders(@PathVariable UUID id) {
        return ApiResponse.success(reminderService.listByFile(id));
    }

    // =====================================================================
    // BCR-03: file boxes, Mark, priority change, reopen.
    // All existing endpoints above are unchanged (backward compatible).
    // =====================================================================

    /** My Drafts / Inbox / Sent / Returned / Closed (Part 4). Self-scoped. */
    @GetMapping("/boxes/{box}")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FileResponse>>> box(
            @PathVariable String box,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginationRequest pagination = new PaginationRequest();
        pagination.setPage(page);
        pagination.setSize(size);
        return ResponseEntity.ok(ApiResponse.success(fileService.box(box, pagination)));
    }

    /** Mark the file to a section/role/user (Part 6). */
    @PostMapping("/{id}/mark")
    @PreAuthorize("hasAuthority('FILE_MARK')")
    public ResponseEntity<ApiResponse<FileResponse>> mark(
            @PathVariable UUID id, @Valid @RequestBody MarkFileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "File marked", fileService.mark(id, request)));
    }

    /** Change priority — Chairman / Commissioner & Secretary only (Part 7). */
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('FILE_PRIORITY_CHANGE')")
    public ResponseEntity<ApiResponse<FileResponse>> changePriority(
            @PathVariable UUID id, @Valid @RequestBody ChangePriorityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Priority changed", fileService.changePriority(id, request)));
    }

    @GetMapping("/{id}/priority-history")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ResponseEntity<ApiResponse<List<PriorityChangeResponse>>> priorityHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(fileService.priorityHistory(id)));
    }

    /** Reopen a closed file — Chairman / Commissioner & Secretary only (Part 8). */
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('FILE_REOPEN')")
    public ResponseEntity<ApiResponse<FileResponse>> reopen(
            @PathVariable UUID id, @Valid @RequestBody ReopenFileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "File reopened", fileService.reopen(id, request)));
    }

    @GetMapping("/{id}/reopen-history")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ResponseEntity<ApiResponse<List<ReopenHistoryResponse>>> reopenHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(fileService.reopenHistory(id)));
    }
}
