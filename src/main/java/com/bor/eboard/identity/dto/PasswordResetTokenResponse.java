package com.bor.eboard.identity.dto;

public record PasswordResetTokenResponse(String resetToken, long expiresIn) {
}
