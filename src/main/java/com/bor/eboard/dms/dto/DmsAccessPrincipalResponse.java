package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.security.DmsPrincipalType;

import java.util.UUID;

public record DmsAccessPrincipalResponse(
        UUID id,
        DmsPrincipalType principalType,
        String code,
        String name,
        UUID departmentId,
        String designationName) {
}
