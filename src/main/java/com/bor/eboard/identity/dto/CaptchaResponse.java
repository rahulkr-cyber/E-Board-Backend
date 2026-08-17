package com.bor.eboard.identity.dto;

public record CaptchaResponse(String captchaId, String imageBase64, long expiresIn) {
}
