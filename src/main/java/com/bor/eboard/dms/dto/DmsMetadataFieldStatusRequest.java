package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotNull;

public record DmsMetadataFieldStatusRequest(@NotNull Boolean active) {
}
