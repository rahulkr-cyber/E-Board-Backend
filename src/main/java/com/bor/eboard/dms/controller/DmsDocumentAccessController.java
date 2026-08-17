package com.bor.eboard.dms.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.dms.constants.DmsPermissions;
import com.bor.eboard.dms.dto.DmsAccessPrincipalResponse;
import com.bor.eboard.dms.dto.DmsDocumentAccessGrantRequest;
import com.bor.eboard.dms.dto.DmsDocumentAccessResponse;
import com.bor.eboard.dms.dto.DmsDocumentShareRequest;
import com.bor.eboard.dms.security.DmsPrincipalType;
import com.bor.eboard.dms.service.DmsAccessPrincipalService;
import com.bor.eboard.dms.service.DmsDocumentPermissionService;
import com.bor.eboard.dms.service.DmsDocumentShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dms")
@Tag(name = "DMS Document Access", description = "DMS object permissions and sharing")
public class DmsDocumentAccessController {

    private final DmsDocumentPermissionService permissionService;
    private final DmsDocumentShareService shareService;
    private final DmsAccessPrincipalService principalService;

    public DmsDocumentAccessController(
            DmsDocumentPermissionService permissionService,
            DmsDocumentShareService shareService,
            DmsAccessPrincipalService principalService) {
        this.permissionService = permissionService;
        this.shareService = shareService;
        this.principalService = principalService;
    }

    @GetMapping("/documents/{documentId}/access")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.SHARE + "', '" + DmsPermissions.AUDIT_VIEW + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Get DMS document permissions and active shares")
    public ApiResponse<DmsDocumentAccessResponse> getAccess(
            @PathVariable UUID documentId) {
        return ApiResponse.success(permissionService.getAccess(documentId));
    }

    @PostMapping("/documents/{documentId}/permissions")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.SHARE + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Grant direct object permissions on a DMS document")
    public ApiResponse<DmsDocumentAccessResponse> grant(
            @PathVariable UUID documentId,
            @Valid @RequestBody DmsDocumentAccessGrantRequest request) {
        return ApiResponse.success(
                "DMS document permission granted successfully",
                permissionService.grant(documentId, request));
    }

    @DeleteMapping("/documents/{documentId}/permissions/{permissionId}")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.SHARE + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Revoke one DMS document permission")
    public ApiResponse<DmsDocumentAccessResponse> revokePermission(
            @PathVariable UUID documentId,
            @PathVariable UUID permissionId) {
        return ApiResponse.success(
                "DMS document permission revoked successfully",
                permissionService.revoke(documentId, permissionId));
    }

    @PostMapping("/documents/{documentId}/shares")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.SHARE + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Share a DMS document with a user, role, department or section")
    public ApiResponse<DmsDocumentAccessResponse> share(
            @PathVariable UUID documentId,
            @Valid @RequestBody DmsDocumentShareRequest request) {
        return ApiResponse.success(
                "DMS document shared successfully",
                shareService.share(documentId, request));
    }

    @DeleteMapping("/documents/{documentId}/shares/{shareId}")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.SHARE + "', '"
            + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Revoke a DMS document share and its permissions")
    public ApiResponse<DmsDocumentAccessResponse> revokeShare(
            @PathVariable UUID documentId,
            @PathVariable UUID shareId) {
        return ApiResponse.success(
                "DMS document share revoked successfully",
                shareService.revoke(documentId, shareId));
    }

    @GetMapping("/access/principals")
    @PreAuthorize("hasAnyAuthority('" + DmsPermissions.VIEW + "', '"
            + DmsPermissions.CREATE + "', '" + DmsPermissions.UPLOAD + "', '"
            + DmsPermissions.DOWNLOAD + "', '" + DmsPermissions.SHARE + "', '"
            + DmsPermissions.AUDIT_VIEW + "', '" + DmsPermissions.ADMIN + "')")
    @Operation(summary = "Search users, roles, departments or sections for DMS sharing")
    public ApiResponse<List<DmsAccessPrincipalResponse>> findPrincipals(
            @RequestParam DmsPrincipalType principalType,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID departmentId) {
        return ApiResponse.success(
                principalService.search(principalType, query, departmentId));
    }
}
