package com.bor.eboard.identity.controller;

import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.identity.dto.*;
import com.bor.eboard.identity.service.CaptchaService;
import com.bor.eboard.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authentication endpoints (04_API_SPEC.md section 2).
 * Controllers only validate and delegate; no business logic here.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh and current user")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "Generate a one-time CAPTCHA image")
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }

    @Operation(summary = "Login with username and password")
    @PostMapping("/login")
    public ApiResponse<OtpChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Credentials verified. OTP sent.", authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ApiResponse.success("Login successful", authService.verifyLoginOtp(request));
    }

    @PostMapping("/resend-otp")
    public ApiResponse<OtpChallengeResponse> resendOtp(@Valid @RequestBody OtpResendRequest request) {
        return ApiResponse.success("OTP resent", authService.resendLoginOtp(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<OtpChallengeResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success("If the account exists, an OTP has been sent.", authService.forgotPassword(request));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ApiResponse<PasswordResetTokenResponse> verifyPasswordResetOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ApiResponse.success("OTP verified", authService.verifyPasswordResetOtp(request));
    }

    @PostMapping("/forgot-password/resend-otp")
    public ApiResponse<OtpChallengeResponse> resendPasswordResetOtp(
            @Valid @RequestBody OtpResendRequest request) {
        return ApiResponse.success("If the account exists, an OTP has been sent.",
                authService.resendPasswordResetOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    public ApiResponse<Void> resetForgottenPassword(@Valid @RequestBody PublicResetPasswordRequest request) {
        authService.resetForgottenPassword(request);
        return ApiResponse.successMessage("Password reset successful. Please sign in.");
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed", authService.refresh(request));
    }

    @Operation(summary = "Logout and revoke refresh tokens")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        UUID userId = currentUserId();
        authService.logout(userId);
        return ApiResponse.successMessage("Logout successful");
    }

    @Operation(summary = "Get the currently authenticated user")
    @GetMapping("/me")
    public ApiResponse<UserInfo> currentUser() {
        UUID userId = currentUserId();
        return ApiResponse.success(authService.currentUser(userId));
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }
}
