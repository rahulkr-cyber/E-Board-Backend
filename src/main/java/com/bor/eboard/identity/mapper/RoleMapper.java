package com.bor.eboard.identity.mapper;

import com.bor.eboard.identity.dto.PermissionResponse;
import com.bor.eboard.identity.dto.RoleResponse;
import com.bor.eboard.identity.entity.Role;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        return toResponse(role, null);
    }

    public RoleResponse toResponse(Role role, List<PermissionResponse> permissions) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.getActive())
                .permissions(permissions)
                .build();
    }
}
