package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DmsMasterDataResolveRequest(
        @NotNull Map<String, Object> parameters) {
}
