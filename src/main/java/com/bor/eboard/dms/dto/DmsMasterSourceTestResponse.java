package com.bor.eboard.dms.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DmsMasterSourceTestResponse(
        boolean successful,
        int resultCount,
        List<DmsMasterDataOptionResponse> options,
        LocalDateTime testedAt) {
}
