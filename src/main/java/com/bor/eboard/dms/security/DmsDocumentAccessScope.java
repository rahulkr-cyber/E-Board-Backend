package com.bor.eboard.dms.security;

import java.util.Set;
import java.util.UUID;

public record DmsDocumentAccessScope(
        UUID userId,
        Set<UUID> roleIds,
        UUID departmentId,
        UUID sectionId,
        boolean administrator) {
}
