package com.bor.eboard.correspondence.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.correspondence.dto.CreateDispatchRequest;
import com.bor.eboard.correspondence.dto.CreateFollowupRequest;
import com.bor.eboard.correspondence.dto.DispatchTargetResponse;
import com.bor.eboard.correspondence.dto.FollowupResponse;
import com.bor.eboard.correspondence.dto.UpdateFollowupRequest;
import com.bor.eboard.correspondence.dto.DispatchRegisterRow;
import com.bor.eboard.correspondence.dto.DispatchResponse;
import com.bor.eboard.correspondence.dto.ReminderResponse;
import com.bor.eboard.correspondence.dto.UpdateDispatchStatusRequest;
import com.bor.eboard.correspondence.dto.UpdateReminderStatusRequest;
import com.bor.eboard.correspondence.service.DispatchService;
import com.bor.eboard.correspondence.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dispatch APIs and dispatch-register report; also reminder status updates.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Dispatch", description = "Outward dispatch register")
public class DispatchController {

    private final DispatchService dispatchService;
    private final ReminderService reminderService;

    @Operation(summary = "Create dispatch record for an outward letter")
    @PostMapping("/dispatches")
    @PreAuthorize("hasAuthority('DISPATCH_CREATE')")
    public ApiResponse<DispatchResponse> create(@Valid @RequestBody CreateDispatchRequest request) {
        return ApiResponse.success("Dispatch created successfully",
                dispatchService.create(request));
    }

    @Operation(summary = "Get dispatch record")
    @GetMapping("/dispatches/{id}")
    @PreAuthorize("hasAuthority('DISPATCH_VIEW')")
    public ApiResponse<DispatchResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(dispatchService.getById(id));
    }

    @Operation(summary = "Update dispatch status")
    @PutMapping("/dispatches/{id}/status")
    @PreAuthorize("hasAuthority('DISPATCH_UPDATE')")
    public ApiResponse<DispatchResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateDispatchStatusRequest request) {
        return ApiResponse.success("Dispatch status updated successfully",
                dispatchService.updateStatus(id, request));
    }

    @Operation(summary = "Search dispatch records")
    @GetMapping("/dispatches")
    @PreAuthorize("hasAuthority('DISPATCH_VIEW')")
    public ApiResponse<PageResponse<DispatchResponse>> search(
            @RequestParam(required = false) String dispatchNumber,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dispatchDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationRequest pagination = new PaginationRequest(page, size, sortBy, sortDir);
        return ApiResponse.success(dispatchService.search(
                dispatchNumber, recipient, mode, status, fromDate, toDate, pagination));
    }

    @Operation(summary = "Dispatch register report for a date range")
    @GetMapping("/reports/dispatch-register")
    @PreAuthorize("hasAuthority('DISPATCH_VIEW')")
    public ApiResponse<List<DispatchRegisterRow>> dispatchRegister(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(dispatchService.dispatchRegister(fromDate, toDate));
    }

    @Operation(summary = "Update a reminder status")
    @PutMapping("/reminders/{id}/status")
    @PreAuthorize("hasAuthority('FILE_VIEW')")
    public ApiResponse<ReminderResponse> updateReminderStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateReminderStatusRequest request) {
        return ApiResponse.success("Reminder status updated successfully",
                reminderService.updateStatus(id, request));
    }

    // =====================================================================
    // BCR-03 Parts 9-10: searchable target picker and follow-ups.
    // Existing endpoints above are unchanged.
    // =====================================================================

    /** Search files and letters to dispatch against — replaces UUID entry. */
    @GetMapping("/dispatches/targets")
    @PreAuthorize("hasAuthority('DISPATCH_CREATE')")
    public ApiResponse<List<DispatchTargetResponse>> searchTargets(@RequestParam String query) {
        return ApiResponse.success(dispatchService.searchTargets(query));
    }

    @GetMapping("/dispatches/{id}/followups")
    @PreAuthorize("hasAuthority('DISPATCH_VIEW')")
    public ApiResponse<List<FollowupResponse>> followups(@PathVariable UUID id) {
        return ApiResponse.success(dispatchService.followups(id));
    }

    @PostMapping("/dispatches/{id}/followups")
    @PreAuthorize("hasAuthority('DISPATCH_UPDATE')")
    public ApiResponse<FollowupResponse> addFollowup(
            @PathVariable UUID id, @Valid @RequestBody CreateFollowupRequest request) {
        return ApiResponse.success("Follow-up recorded", dispatchService.addFollowup(id, request));
    }

    @PatchMapping("/dispatches/followups/{followupId}")
    @PreAuthorize("hasAuthority('DISPATCH_UPDATE')")
    public ApiResponse<FollowupResponse> updateFollowup(
            @PathVariable UUID followupId, @RequestBody UpdateFollowupRequest request) {
        return ApiResponse.success("Follow-up updated",
                dispatchService.updateFollowup(followupId, request));
    }
}
