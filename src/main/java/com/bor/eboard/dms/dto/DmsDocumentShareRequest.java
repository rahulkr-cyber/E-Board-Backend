package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.security.DmsDocumentAccessLevel;
import com.bor.eboard.dms.security.DmsPrincipalType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record DmsDocumentShareRequest(
        @NotNull DmsPrincipalType principalType,
        @NotNull UUID principalId,
        @NotEmpty Set<DmsDocumentAccessLevel> accessLevels,
        LocalDateTime expiresAt,
        @Size(max = 1000) String note) {
}
