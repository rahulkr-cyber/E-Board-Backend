package com.bor.eboard.correspondence.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.correspondence.dto.AttachmentInfo;
import com.bor.eboard.correspondence.dto.CreateLetterRequest;
import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.LetterResponse;
import com.bor.eboard.correspondence.dto.UpdateFollowupRequest;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.service.FollowupService;
import com.bor.eboard.correspondence.service.LetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.correspondence.dto.LetterMovementResponse;
import com.bor.eboard.correspondence.dto.LetterActionRequest;
import com.bor.eboard.correspondence.dto.MarkLetterRequest;
import com.bor.eboard.correspondence.dto.UpdateLetterRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Letter APIs (04_API_SPEC.md 7.4-7.5) and follow-up status updates.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Letters", description = "Inward/outward/internal letters")
public class LetterController {

    private final LetterService letterService;
    private final FollowupService followupService;

    @Operation(summary = "Create letter")
    @PostMapping("/letters")
    @PreAuthorize("hasAuthority('LETTER_CREATE')")
    public ApiResponse<LetterResponse> create(@Valid @RequestBody CreateLetterRequest request) {
        return ApiResponse.success("Letter created successfully", letterService.create(request));
    }

    @Operation(summary = "Get letter with attachments")
    @GetMapping("/letters/{id}")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ApiResponse<LetterResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(letterService.getById(id));
    }

    @Operation(summary = "Upload letter attachment (multipart)")
    @PostMapping("/letters/{letterId}/attachments")
    @PreAuthorize("hasAuthority('LETTER_CREATE')")
    public ApiResponse<AttachmentInfo> uploadAttachment(
            @PathVariable UUID letterId,
            @RequestParam(required = false) UUID documentTypeId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Attachment uploaded successfully",
                letterService.uploadAttachment(letterId, documentTypeId, file));
    }

    @Operation(summary = "Create a follow-up on a letter")
    @PostMapping("/letters/{id}/followups")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ApiResponse<FollowupResponse> createLetterFollowup(
            @PathVariable UUID id, @Valid @RequestBody CreateFollowupRequest request) {
        return ApiResponse.success("Follow-up created successfully",
                followupService.createForLetter(id, request));
    }

    @Operation(summary = "List follow-ups on a letter")
    @GetMapping("/letters/{id}/followups")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ApiResponse<List<FollowupResponse>> letterFollowups(@PathVariable UUID id) {
        return ApiResponse.success(followupService.listByLetter(id));
    }

    @Operation(summary = "Update a follow-up (record reply / change status)")
    @PutMapping("/followups/{id}")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<FollowupResponse> updateFollowup(
            @PathVariable UUID id, @Valid @RequestBody UpdateFollowupRequest request) {
        return ApiResponse.success("Follow-up updated successfully",
                followupService.update(id, request));
    }

    // =====================================================================
    // BCR-03 Part 16: letter boxes and the Mark action.
    // Existing endpoints above are unchanged.
    // =====================================================================

    /** My Draft / Inbox / Sent / Returned / Closed Letters. Self-scoped. */
    @GetMapping("/letters/boxes/{box}")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<LetterResponse>>> letterBox(
            @PathVariable String box,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginationRequest pagination = new PaginationRequest();
        pagination.setPage(page);
        pagination.setSize(size);
        return ResponseEntity.ok(ApiResponse.success(letterService.box(box, pagination)));
    }

    @PostMapping("/letters/{id}/mark")
    @PreAuthorize("hasAuthority('LETTER_MARK')")
    public ResponseEntity<ApiResponse<LetterResponse>> markLetter(
            @PathVariable UUID id, @Valid @RequestBody MarkLetterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Letter marked", letterService.mark(id, request)));
    }

    @GetMapping("/letters/{id}/movements")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ResponseEntity<ApiResponse<List<LetterMovementResponse>>> letterMovements(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(letterService.movements(id)));
    }

    // =====================================================================
    // Letter lifecycle. The movement trail already declared MARK | RETURN |
    // CLOSE | REOPEN; these endpoints complete it. Existing endpoints above
    // are unchanged.
    // =====================================================================

    /** General search — distinct from the five personal boxes. */
    @GetMapping("/letters")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ApiResponse<PageResponse<LetterResponse>> searchLetters(
            @RequestParam(required = false) String letterNumber,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String letterType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID fileId,
            @RequestParam(required = false) UUID checklistTemplateId,
            @RequestParam(required = false) String checklistStatus,
            @RequestParam(required = false) Boolean checklistComplete,
            @RequestParam(required = false) Boolean missingMandatory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        PaginationRequest pagination = new PaginationRequest();
        pagination.setPage(page);
        pagination.setSize(size);
        return ApiResponse.success(letterService.search(
                letterNumber, subject, direction, letterType, status, sectionId, fileId,
                checklistTemplateId, checklistStatus, checklistComplete, missingMandatory, pagination));
    }

    /** Correct a draft letter's metadata. Owner-only, DRAFT-only. */
    @PutMapping("/letters/{id}")
    @PreAuthorize("hasAuthority('LETTER_UPDATE')")
    public ApiResponse<LetterResponse> updateLetter(
            @PathVariable UUID id, @Valid @RequestBody UpdateLetterRequest request) {
        return ApiResponse.success("Letter updated", letterService.update(id, request));
    }

    /** Mark the letter back to its previous holder for correction. */
    @PostMapping("/letters/{id}/return")
    @PreAuthorize("hasAuthority('LETTER_MARK')")
    public ApiResponse<LetterResponse> returnLetter(
            @PathVariable UUID id, @Valid @RequestBody LetterActionRequest request) {
        return ApiResponse.success("Letter returned", letterService.returnLetter(id, request));
    }

    @PostMapping("/letters/{id}/close")
    @PreAuthorize("hasAuthority('LETTER_CLOSE')")
    public ApiResponse<LetterResponse> closeLetter(
            @PathVariable UUID id, @Valid @RequestBody LetterActionRequest request) {
        return ApiResponse.success("Letter closed", letterService.close(id, request));
    }

    @PostMapping("/letters/{id}/reopen")
    @PreAuthorize("hasAuthority('LETTER_REOPEN')")
    public ApiResponse<LetterResponse> reopenLetter(
            @PathVariable UUID id, @Valid @RequestBody LetterActionRequest request) {
        return ApiResponse.success("Letter reopened", letterService.reopen(id, request));
    }

    /** An internal note: a remark recorded against the letter without moving it. */
    @PostMapping("/letters/{id}/notes")
    @PreAuthorize("hasAuthority('LETTER_VIEW')")
    public ApiResponse<LetterMovementResponse> addNote(
            @PathVariable UUID id, @Valid @RequestBody LetterActionRequest request) {
        return ApiResponse.success("Note recorded", letterService.addNote(id, request));
    }
}
