package com.bor.eboard.identity.service;

import com.bor.eboard.identity.dto.PermissionResponse;

import java.util.List;

public interface PermissionService {

    List<PermissionResponse> getAll();
}
