package com.bor.eboard.dms.dto;

import com.bor.eboard.dms.security.DmsDocumentAccessLevel;

import java.util.Set;

public record DmsDocumentAccessSummaryResponse(
        boolean owner,
        boolean administrator,
        Set<DmsDocumentAccessLevel> effectiveAccessLevels) {
}
