package com.bor.eboard.identity.service;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.constants.AppConstants;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.identity.dto.OtpChallengeResponse;
import com.bor.eboard.identity.entity.AuthenticationOtp;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.repository.AuthenticationOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {
    public static final String LOGIN = "LOGIN";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    private final AuthenticationOtpRepository repository;
    private final SmsService smsService;
    private final CryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.otp.expiry-seconds:300}") private long expirySeconds;
    @Value("${security.otp.max-retries:5}") private int maxRetries;
    @Value("${security.otp.max-resends:3}") private int maxResends;
    @Value("${security.otp.length:6}") private int otpLength;
    @Value("${security.otp.request-interval-seconds:30}") private long requestIntervalSeconds;
    @Value("${security.password-reset.token-expiry-seconds:600}") private long resetTokenExpirySeconds;

    @Transactional(noRollbackFor = IllegalStateException.class)
    public OtpChallengeResponse create(User user, String purpose) {
        repository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(previous -> previous.getCreatedAt().plusSeconds(requestIntervalSeconds)
                        .isAfter(LocalDateTime.now()))
                .ifPresent(previous -> { throw new UnauthorizedException("Please wait before requesting another OTP"); });
        String encryptedOtp = smsService.requestOtp(user.getMobile(), purpose);
        String otp = normalizeGatewayOtp(encryptedOtp);
        repository.consumeActive(user.getId(), purpose);
        AuthenticationOtp challenge = new AuthenticationOtp();
        challenge.setUserId(user.getId());
        challenge.setPurpose(purpose);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setMobileLastFour(lastFour(user.getMobile()));
        challenge.setExpiresAt(LocalDateTime.now().plusSeconds(expirySeconds));
        challenge = repository.save(challenge);
        auditService.recordAuth(user.getId(), user.getUsername(),
                AppConstants.AUDIT_ACTION_OTP_GENERATED, true, purpose);
        return response(challenge, encryptedOtp);
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public OtpChallengeResponse resend(UUID challengeId, String purpose, User user) {
        AuthenticationOtp challenge = find(challengeId, purpose);
        requireOwner(challenge, user);
        if (Boolean.TRUE.equals(challenge.getConsumed()) || Boolean.TRUE.equals(challenge.getVerified()))
            throw new UnauthorizedException("OTP challenge is no longer valid");
        if (challenge.getResendCount() >= maxResends)
            throw new UnauthorizedException("Maximum OTP resend limit reached");
        String encryptedOtp = smsService.requestOtp(user.getMobile(), purpose);
        String otp = normalizeGatewayOtp(encryptedOtp);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setAttempts(0);
        challenge.setResendCount(challenge.getResendCount() + 1);
        challenge.setExpiresAt(LocalDateTime.now().plusSeconds(expirySeconds));
        repository.save(challenge);
        auditService.recordAuth(user.getId(), user.getUsername(),
                AppConstants.AUDIT_ACTION_OTP_RESENT, true, purpose);
        return response(challenge, encryptedOtp);
    }

    @Transactional
    public AuthenticationOtp verify(UUID challengeId, String enteredOtp, String encryptedOtp,
                                    String purpose, User user) {
        AuthenticationOtp challenge = find(challengeId, purpose);
        requireOwner(challenge, user);
        if (Boolean.TRUE.equals(challenge.getConsumed()) || Boolean.TRUE.equals(challenge.getVerified()))
            throw new UnauthorizedException("OTP has already been used");
        if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
            challenge.setConsumed(Boolean.TRUE);
            repository.save(challenge);
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_OTP_EXPIRED, false, purpose);
            throw new UnauthorizedException("OTP has expired. Request a new OTP.");
        }
        if (challenge.getAttempts() >= maxRetries)
            throw new UnauthorizedException("Maximum OTP verification attempts exceeded");
        String decryptedOtp;
        try {
            decryptedOtp = normalizeGatewayOtp(encryptedOtp);
        } catch (IllegalStateException ex) {
            decryptedOtp = "";
        }
        if (!enteredOtp.equals(decryptedOtp)
                || !passwordEncoder.matches(enteredOtp, challenge.getOtpHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= maxRetries) challenge.setConsumed(Boolean.TRUE);
            repository.save(challenge);
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_OTP_FAILED, false, "Invalid OTP: " + purpose);
            throw new UnauthorizedException("Invalid OTP");
        }
        challenge.setVerified(Boolean.TRUE);
        auditService.recordAuth(user.getId(), user.getUsername(),
                AppConstants.AUDIT_ACTION_OTP_VERIFIED, true, purpose);
        return repository.save(challenge);
    }

    @Transactional
    public String issueResetToken(AuthenticationOtp challenge) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        challenge.setResetTokenHash(hashToken(token));
        challenge.setExpiresAt(LocalDateTime.now().plusSeconds(resetTokenExpirySeconds));
        repository.save(challenge);
        return token;
    }

    @Transactional
    public AuthenticationOtp findResetToken(String token) {
        AuthenticationOtp challenge = repository
                .findByResetTokenHashAndPurposeAndVerifiedTrueAndConsumedFalse(hashToken(token), PASSWORD_RESET)
                .filter(o -> !LocalDateTime.now().isAfter(o.getExpiresAt()))
                .orElseThrow(() -> new UnauthorizedException("Password reset session is invalid or expired"));
        return challenge;
    }

    private AuthenticationOtp find(UUID id, String purpose) {
        return repository.findByIdAndPurpose(id, purpose)
                .orElseThrow(() -> new UnauthorizedException("OTP challenge is invalid or expired"));
    }
    private void requireOwner(AuthenticationOtp challenge, User user) {
        if (!challenge.getUserId().equals(user.getId())) throw new UnauthorizedException("OTP challenge is invalid");
    }
    private OtpChallengeResponse response(AuthenticationOtp c, String encryptedOtp) {
        return new OtpChallengeResponse(c.getId(), encryptedOtp,
                "******" + c.getMobileLastFour(), expirySeconds,
                Math.max(0, maxResends - c.getResendCount()));
    }
    private String lastFour(String mobile) { return mobile.substring(mobile.length() - 4); }
    public long getExpirySeconds() { return expirySeconds; }
    public long getResetTokenExpirySeconds() { return resetTokenExpirySeconds; }
    public int getMaxResends() { return maxResends; }
    public String createDecoyEncryptedOtp() {
        StringBuilder otp = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) otp.append(secureRandom.nextInt(10));
        return cryptoService.encryptOtp(otp.toString());
    }
    private String normalizeGatewayOtp(String gatewayResponse) {
        if (gatewayResponse == null || gatewayResponse.isBlank())
            throw new IllegalStateException("Government SMS gateway returned an empty OTP response");
        String otp = gatewayResponse.trim();
        if (!otp.matches("^[0-9]{" + otpLength + "}$")) {
            otp = cryptoService.decryptOtp(otp).trim();
        }
        if (!otp.matches("^[0-9]{" + otpLength + "}$"))
            throw new IllegalStateException("Government SMS gateway returned an invalid OTP response");
        return otp;
    }
    private String hashToken(String token) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
