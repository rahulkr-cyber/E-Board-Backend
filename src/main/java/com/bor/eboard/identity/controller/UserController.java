package com.bor.eboard.identity.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.identity.dto.AssignRolesRequest;
import com.bor.eboard.identity.dto.ChangeUserStatusRequest;
import com.bor.eboard.identity.dto.CreateUserRequest;
import com.bor.eboard.identity.dto.ResetPasswordRequest;
import com.bor.eboard.identity.dto.UpdateUserRequest;
import com.bor.eboard.identity.dto.UserResponse;
import com.bor.eboard.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * User management endpoints (04_API_SPEC.md section 3).
 * Every write is guarded by a permission (09_SECURITY.md section 5).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create user")
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created successfully", userService.create(request));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ApiResponse<UserResponse> update(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("User updated successfully", userService.update(id, request));
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(userService.getById(id));
    }

    @Operation(summary = "Search users with pagination")
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PaginationRequest pagination = new PaginationRequest(page, size, sortBy, sortDir);
        return ApiResponse.success(
                userService.search(query, sectionId, departmentId, status, pagination));
    }

    @Operation(summary = "Change user status (ACTIVE / INACTIVE / SUSPENDED)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_STATUS_CHANGE')")
    public ApiResponse<UserResponse> changeStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody ChangeUserStatusRequest request) {
        return ApiResponse.success("User status updated", userService.changeStatus(id, request));
    }

    @Operation(summary = "Assign roles to user (replaces current active roles)")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    public ApiResponse<UserResponse> assignRoles(@PathVariable UUID id,
                                                 @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.success("Roles assigned", userService.assignRoles(id, request));
    }

    @Operation(summary = "Upload or replace employee profile photograph")
    @PostMapping("/{id}/profile-photo")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ApiResponse<UserResponse> uploadProfilePhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Profile photograph updated",
                userService.uploadProfilePhoto(id, file));
    }

    @Operation(summary = "Reset user password (admin action)")
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ApiResponse<Void> resetPassword(@PathVariable UUID id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.successMessage("Password reset successfully");
    }

    @Operation(summary = "Unlock a locked user account")
    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER_STATUS_CHANGE')")
    public ApiResponse<Void> unlock(@PathVariable UUID id) {
        userService.unlockAccount(id);
        return ApiResponse.successMessage("Account unlocked");
    }
}
