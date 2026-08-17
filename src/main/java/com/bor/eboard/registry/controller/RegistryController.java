package com.bor.eboard.registry.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.registry.dto.AttachmentResponse;
import com.bor.eboard.registry.dto.CreateDiaryEntryRequest;
import com.bor.eboard.registry.dto.DiaryEntryResponse;
import com.bor.eboard.registry.dto.DiaryRegisterRow;
import com.bor.eboard.registry.dto.ForwardDiaryRequest;
import com.bor.eboard.registry.dto.UpdateDiaryEntryRequest;
import com.bor.eboard.registry.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registry APIs (04_API_SPEC.md section 6).
 * Registry is intake-only; approve/reject/close do not exist here by design.
 */
@RestController
@RequestMapping("/api/v1/registry")
@RequiredArgsConstructor
@Tag(name = "Registry", description = "Diary entry, forwarding, receipt, diary register")
public class RegistryController {

    private final DiaryService diaryService;

    @Operation(summary = "Create diary entry (generates immutable diary number)")
    @PostMapping("/diary")
    @PreAuthorize("hasAuthority('DIARY_CREATE')")
    public ApiResponse<DiaryEntryResponse> create(
            @Valid @RequestBody CreateDiaryEntryRequest request) {
        return ApiResponse.success("Diary entry created successfully",
                diaryService.create(request));
    }

    @Operation(summary = "Update diary metadata (allowed only until first forward)")
    @PutMapping("/diary/{id}")
    @PreAuthorize("hasAuthority('DIARY_CREATE')")
    public ApiResponse<DiaryEntryResponse> updateMetadata(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiaryEntryRequest request) {
        return ApiResponse.success("Diary entry updated successfully",
                diaryService.updateMetadata(id, request));
    }

    @Operation(summary = "Get diary entry with receipt and attachments")
    @GetMapping("/diary/{id}")
    @PreAuthorize("hasAuthority('DIARY_VIEW')")
    public ApiResponse<DiaryEntryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(diaryService.getById(id));
    }

    @Operation(summary = "Search diary entries")
    @GetMapping("/diary")
    @PreAuthorize("hasAuthority('DIARY_VIEW')")
    public ApiResponse<PageResponse<DiaryEntryResponse>> search(
            @RequestParam(required = false) String diaryNumber,
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID checklistTemplateId,
            @RequestParam(required = false) String checklistStatus,
            @RequestParam(required = false) Boolean checklistComplete,
            @RequestParam(required = false) Boolean missingMandatory,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationRequest pagination = new PaginationRequest(page, size, sortBy, sortDir);
        return ApiResponse.success(diaryService.search(
                diaryNumber, sender, subject, status, sectionId, fromDate, toDate,
                checklistTemplateId, checklistStatus, checklistComplete, missingMandatory, pagination));
    }

    @Operation(summary = "Forward diary entry to a section (locks metadata)")
    @PostMapping("/diary/{id}/forward")
    @PreAuthorize("hasAuthority('DIARY_FORWARD')")
    public ApiResponse<DiaryEntryResponse> forward(
            @PathVariable UUID id,
            @Valid @RequestBody ForwardDiaryRequest request) {
        return ApiResponse.success("Diary entry forwarded successfully",
                diaryService.forward(id, request));
    }

    @Operation(summary = "Cancel a diary entry (only before forwarding)")
    @PostMapping("/diary/{id}/cancel")
    @PreAuthorize("hasAuthority('DIARY_CREATE')")
    public ApiResponse<DiaryEntryResponse> cancel(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success("Diary entry cancelled",
                diaryService.cancel(id, body.get("reason")));
    }

    @Operation(summary = "Upload diary attachment (multipart)")
    @PostMapping("/diary/{diaryId}/attachments")
    @PreAuthorize("hasAuthority('DIARY_CREATE')")
    public ApiResponse<AttachmentResponse> uploadAttachment(
            @PathVariable UUID diaryId,
            @RequestParam(required = false) UUID documentTypeId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Attachment uploaded successfully",
                diaryService.uploadAttachment(diaryId, documentTypeId, file));
    }

    @Operation(summary = "Diary register report for a date range")
    @GetMapping("/reports/diary-register")
    @PreAuthorize("hasAuthority('DIARY_VIEW')")
    public ApiResponse<List<DiaryRegisterRow>> diaryRegister(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(diaryService.diaryRegister(fromDate, toDate));
    }
}
