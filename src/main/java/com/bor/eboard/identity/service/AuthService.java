package com.bor.eboard.identity.service;

import com.bor.eboard.identity.dto.LoginRequest;
import com.bor.eboard.identity.dto.*;
import com.bor.eboard.identity.dto.RefreshTokenRequest;
import com.bor.eboard.identity.dto.UserInfo;

import java.util.UUID;

public interface AuthService {

    OtpChallengeResponse login(LoginRequest request);

    LoginResponse verifyLoginOtp(OtpVerifyRequest request);

    OtpChallengeResponse resendLoginOtp(OtpResendRequest request);

    OtpChallengeResponse forgotPassword(ForgotPasswordRequest request);

    PasswordResetTokenResponse verifyPasswordResetOtp(OtpVerifyRequest request);

    OtpChallengeResponse resendPasswordResetOtp(OtpResendRequest request);

    void resetForgottenPassword(PublicResetPasswordRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(UUID userId);

    UserInfo currentUser(UUID userId);
}
