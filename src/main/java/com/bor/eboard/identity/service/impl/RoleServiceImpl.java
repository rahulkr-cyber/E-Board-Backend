package com.bor.eboard.identity.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.constants.AppConstants;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.identity.dto.AssignPermissionsRequest;
import com.bor.eboard.identity.dto.CreateRoleRequest;
import com.bor.eboard.identity.dto.PermissionResponse;
import com.bor.eboard.identity.dto.RoleResponse;
import com.bor.eboard.identity.dto.UpdateRoleRequest;
import com.bor.eboard.identity.entity.Permission;
import com.bor.eboard.identity.entity.Role;
import com.bor.eboard.identity.entity.RolePermission;
import com.bor.eboard.identity.mapper.PermissionMapper;
import com.bor.eboard.identity.mapper.RoleMapper;
import com.bor.eboard.identity.repository.PermissionRepository;
import com.bor.eboard.identity.repository.RolePermissionRepository;
import com.bor.eboard.identity.repository.RoleRepository;
import com.bor.eboard.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "Role code already exists: " + request.getCode());
        }
        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setActive(Boolean.TRUE);
        role = roleRepository.save(role);

        auditService.record(AppConstants.MODULE_IDENTITY, "ROLE", role.getId(),
                AppConstants.AUDIT_ACTION_CREATE, null, "code=" + role.getCode());

        return roleMapper.toResponse(role, List.of());
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        Role role = findRole(id);
        String oldValue = "name=" + role.getName() + ", active=" + role.getActive();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setActive(request.getActive());
        role = roleRepository.save(role);

        auditService.record(AppConstants.MODULE_IDENTITY, "ROLE", role.getId(),
                AppConstants.AUDIT_ACTION_UPDATE, oldValue,
                "name=" + role.getName() + ", active=" + role.getActive());

        return roleMapper.toResponse(role, resolvePermissions(role.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {
        Role role = findRole(id);
        return roleMapper.toResponse(role, resolvePermissions(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(role -> roleMapper.toResponse(role, resolvePermissions(role.getId())))
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(UUID roleId, AssignPermissionsRequest request) {
        Role role = findRole(roleId);

        List<UUID> requestedIds = request.getPermissionIds().stream().distinct().toList();
        List<Permission> permissions =
                permissionRepository.findByIdInAndDeletedFalse(requestedIds);
        System.out.println("Requested IDs");
        requestedIds.forEach(System.out::println);

        System.out.println("Permissions returned");
        permissions.forEach(p -> System.out.println(p.getId()));
        if (permissions.size() != requestedIds.size()) {
            throw new ValidationException("One or more permission references are invalid");
        }

        List<RolePermission> existing = rolePermissionRepository.findByRoleId(roleId);
        String oldValue = existing.stream()
                .map(rp -> rp.getPermissionId().toString())
                .collect(Collectors.joining(","));

        rolePermissionRepository.deleteAll(existing);
       
        rolePermissionRepository.flush();  
        
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permission.getId());
            rolePermissionRepository.save(rolePermission);
        }

        String newValue = permissions.stream()
                .map(p -> p.getId().toString())
                .collect(Collectors.joining(","));
        auditService.record(AppConstants.MODULE_IDENTITY, "ROLE", roleId,
                AppConstants.AUDIT_ACTION_PERMISSION_CHANGE, oldValue, newValue);

        return roleMapper.toResponse(role, resolvePermissions(roleId));
    }

    private Role findRole(UUID id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    private List<PermissionResponse> resolvePermissions(UUID roleId) {
        List<UUID> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findByIdInAndDeletedFalse(permissionIds).stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}
