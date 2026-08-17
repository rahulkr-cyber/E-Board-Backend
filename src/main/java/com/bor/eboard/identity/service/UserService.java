package com.bor.eboard.identity.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.identity.dto.AssignRolesRequest;
import com.bor.eboard.identity.dto.ChangeUserStatusRequest;
import com.bor.eboard.identity.dto.CreateUserRequest;
import com.bor.eboard.identity.dto.ResetPasswordRequest;
import com.bor.eboard.identity.dto.UpdateUserRequest;
import com.bor.eboard.identity.dto.UserResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse update(UUID id, UpdateUserRequest request);

    UserResponse getById(UUID id);

    PageResponse<UserResponse> search(String query, UUID sectionId, UUID departmentId,
                                      String status, PaginationRequest pagination);

    UserResponse changeStatus(UUID id, ChangeUserStatusRequest request);

    UserResponse assignRoles(UUID id, AssignRolesRequest request);

    UserResponse uploadProfilePhoto(UUID id, MultipartFile file);

    void resetPassword(UUID id, ResetPasswordRequest request);

    void unlockAccount(UUID id);
}
