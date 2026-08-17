package com.bor.eboard.identity.controller;

import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.identity.dto.AssignPermissionsRequest;
import com.bor.eboard.identity.dto.CreateRoleRequest;
import com.bor.eboard.identity.dto.PermissionResponse;
import com.bor.eboard.identity.dto.RoleResponse;
import com.bor.eboard.identity.dto.UpdateRoleRequest;
import com.bor.eboard.identity.service.PermissionService;
import com.bor.eboard.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Role and permission endpoints (04_API_SPEC.md section 4).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions", description = "RBAC administration")
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @Operation(summary = "List all roles")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.success(roleService.getAll());
    }

    @Operation(summary = "Get role by id with permissions")
    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ApiResponse<RoleResponse> getRole(@PathVariable UUID id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @Operation(summary = "Create role")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success("Role created successfully", roleService.create(request));
    }

    @Operation(summary = "Update role")
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ApiResponse<RoleResponse> updateRole(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success("Role updated successfully", roleService.update(id, request));
    }

    @Operation(summary = "Assign permissions to role (replaces current set)")
    @PostMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_ASSIGN')")
    public ApiResponse<RoleResponse> assignPermissions(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResponse.success("Permissions assigned",
                roleService.assignPermissions(id, request));
    }

    @Operation(summary = "List all permissions")
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_VIEW')")
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        return ApiResponse.success(permissionService.getAll());
    }
}
