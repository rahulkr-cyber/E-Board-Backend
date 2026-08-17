package com.bor.eboard.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OtpVerifyRequest(@NotNull UUID challengeId,
                               @NotBlank String enteredOtp,
                               @NotBlank String encryptedOtp) {
}
