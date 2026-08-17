package com.bor.eboard.dms.dto;

import jakarta.validation.constraints.NotNull;

public record DmsMasterSourceStatusRequest(@NotNull Boolean active) {
}
