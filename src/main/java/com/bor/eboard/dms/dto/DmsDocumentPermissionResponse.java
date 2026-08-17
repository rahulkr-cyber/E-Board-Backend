package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsPrincipalType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmsDocumentPermissionResponse(
        UUID id,
        UUID shareId,
        DmsPrincipalType principalType,
        UUID principalId,
        String principalName,
        DmsDocumentAccessLevel accessLevel,
        LocalDateTime expiresAt,
        boolean active,
        UUID grantedBy,
        LocalDateTime grantedAt) {
}
