package com.bor.eboard.identity.service.impl;

import com.bor.eboard.identity.dto.PermissionResponse;
import com.bor.eboard.identity.mapper.PermissionMapper;
import com.bor.eboard.identity.repository.PermissionRepository;
import com.bor.eboard.identity.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository.findByDeletedFalseOrderByModuleAscCodeAsc().stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}
