package com.bor.eboard.charge.controller;

import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.CreateChargeRequest;
import com.bor.eboard.charge.service.ChargeService;
import com.bor.eboard.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * Charge management APIs (04_API_SPEC.md section 14).
 */
@RestController
@RequestMapping("/api/v1/charge")
@RequiredArgsConstructor
@Tag(name = "Charge Management", description = "Temporary/additional/full charge assignments")
public class ChargeController {

    private final ChargeService chargeService;

    @Operation(summary = "Create a charge assignment")
    @PostMapping
    @PreAuthorize("hasAuthority('CHARGE_MANAGE')")
    public ApiResponse<ChargeResponse> create(@Valid @RequestBody CreateChargeRequest request) {
        return ApiResponse.success("Charge assigned successfully", chargeService.create(request));
    }

    @Operation(summary = "List active charge assignments")
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('POSTING_VIEW') or hasAuthority('CHARGE_MANAGE')")
    public ApiResponse<List<ChargeResponse>> active() {
        return ApiResponse.success(chargeService.getActive());
    }

    @Operation(summary = "Charges held by a user")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('POSTING_VIEW') or hasAuthority('CHARGE_MANAGE')")
    public ApiResponse<List<ChargeResponse>> byUser(@PathVariable UUID userId) {
        return ApiResponse.success(chargeService.getByUser(userId));
    }

    @Operation(summary = "Cancel a charge assignment")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CHARGE_MANAGE')")
    public ApiResponse<ChargeResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Charge cancelled", chargeService.cancel(id));
    }

    @Operation(summary = "Expire a charge assignment")
    @PatchMapping("/{id}/expire")
    @PreAuthorize("hasAuthority('CHARGE_MANAGE')")
    public ApiResponse<ChargeResponse> expire(@PathVariable UUID id) {
        return ApiResponse.success("Charge expired", chargeService.expire(id));
    }

    /**
     * Upload — or replace — the Government Order / Office Order PDF for a
     * charge. Reuses the shared attachment storage; the document is then
     * downloadable through the existing
     * {@code GET /api/v1/files/storage/{attachmentId}/download} endpoint.
     */
    @Operation(summary = "Upload or replace the charge order document (GO / Office Order)")
    @PostMapping("/{id}/order-document")
    @PreAuthorize("hasAuthority('CHARGE_MANAGE')")
    public ApiResponse<ChargeResponse> uploadOrderDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Order document uploaded",
                chargeService.uploadOrderDocument(id, file));
    }
}
