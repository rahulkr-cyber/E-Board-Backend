package com.bor.eboard.identity.dto;

import java.util.UUID;

public record OtpChallengeResponse(UUID challengeId, String encryptedOtp, String maskedMobile,
                                   long expiresIn, int resendsRemaining) {
}
