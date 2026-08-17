package com.bor.eboard.register.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.register.dto.RegisterRow;
import com.bor.eboard.register.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * BCR-03 Part 11. Two registers over the same live data:
 * a Section register (scoped to one section) and a Central register (all
 * sections), the latter reserved for Chairman / Commissioner & Secretary.
 */
@RestController
@RequestMapping("/api/v1/registers")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    /** Section File Register. */
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyAuthority('REGISTER_SECTION_VIEW', 'REGISTER_CENTRAL_VIEW')")
    public ApiResponse<PageResponse<RegisterRow>> sectionRegister(
            @PathVariable UUID sectionId,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) UUID priorityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(registerService.register(
                sectionId, ownerId, priorityId, status, fromDate, toDate,
                pagination(page, size)));
    }

    /** Central File Register — all sections. */
    @GetMapping("/central")
    @PreAuthorize("hasAuthority('REGISTER_CENTRAL_VIEW')")
    public ApiResponse<PageResponse<RegisterRow>> centralRegister(
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) UUID priorityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(registerService.register(
                sectionId, ownerId, priorityId, status, fromDate, toDate,
                pagination(page, size)));
    }

    private PaginationRequest pagination(int page, int size) {
        PaginationRequest pagination = new PaginationRequest();
        pagination.setPage(page);
        pagination.setSize(size);
        return pagination;
    }
}
