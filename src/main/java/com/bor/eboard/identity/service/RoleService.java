package com.bor.eboard.identity.service;

import com.bor.eboard.identity.dto.AssignPermissionsRequest;
import com.bor.eboard.identity.dto.CreateRoleRequest;
import com.bor.eboard.identity.dto.RoleResponse;
import com.bor.eboard.identity.dto.UpdateRoleRequest;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    RoleResponse create(CreateRoleRequest request);

    RoleResponse update(UUID id, UpdateRoleRequest request);

    RoleResponse getById(UUID id);

    List<RoleResponse> getAll();

    RoleResponse assignPermissions(UUID roleId, AssignPermissionsRequest request);
}
