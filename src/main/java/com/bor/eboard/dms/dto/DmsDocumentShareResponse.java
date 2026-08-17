package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsPrincipalType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record DmsDocumentShareResponse(
        UUID id,
        DmsPrincipalType principalType,
        UUID principalId,
        String principalName,
        Set<DmsDocumentAccessLevel> accessLevels,
        UUID sharedBy,
        String sharedByName,
        LocalDateTime sharedAt,
        LocalDateTime expiresAt,
        String note,
        boolean active) {
}
