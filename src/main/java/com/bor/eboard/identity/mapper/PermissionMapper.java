package com.bor.eboard.identity.mapper;

import com.bor.eboard.identity.dto.PermissionResponse;
import com.bor.eboard.identity.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }
}
