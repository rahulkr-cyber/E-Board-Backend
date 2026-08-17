package com.bor.eboard.dms.dto;

import java.util.Map;

public record DmsMasterDataOptionResponse(
        String value,
        String label,
        Map<String, Object> attributes) {
}
