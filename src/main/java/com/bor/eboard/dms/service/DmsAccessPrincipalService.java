package com.bor.eboard.dms.service;

import com.bor.eboard.dms.dto.DmsAccessPrincipalResponse;
import com.bor.eboard.dms.security.DmsPrincipalType;

import java.util.List;
import java.util.UUID;

public interface DmsAccessPrincipalService {

    List<DmsAccessPrincipalResponse> search(
            DmsPrincipalType principalType,
            String query,
            UUID departmentId);

    String requireName(DmsPrincipalType principalType, UUID principalId);
}
