package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OtpResendRequest(@NotNull UUID challengeId) {
}
